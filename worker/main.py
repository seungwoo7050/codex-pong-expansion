"""
[모듈] worker/main.py
설명:
  - v0.13.0에서 GPU 기반 FFmpeg 인코더를 우선 적용하되, 미지원 시 자동으로 CPU(libx264) 경로로 폴백한다.
  - Redis Streams 잡 큐를 구독해 리플레이를 MP4/PNG로 변환하고, 지원 하드웨어 정보를 시작 시 로깅한다.
버전: v0.13.0
관련 설계문서:
  - design/backend/v0.13.0-export-hw-accel-flags.md
"""

import json
import logging
import os
import subprocess
import threading
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable, List, Optional

import redis
from PIL import Image, ImageDraw, ImageFont

REDIS_HOST = os.getenv("REDIS_HOST", "redis")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REQUEST_STREAM = os.getenv("JOB_QUEUE_REQUEST_STREAM", "job.requests")
PROGRESS_STREAM = os.getenv("JOB_QUEUE_PROGRESS_STREAM", "job.progress")
RESULT_STREAM = os.getenv("JOB_QUEUE_RESULT_STREAM", "job.results")
CONSUMER_GROUP = os.getenv("JOB_QUEUE_CONSUMER_GROUP", "replay-jobs")
CONSUMER_NAME = os.getenv("WORKER_ID", "replay-worker")

EXPORT_ROOT = Path(os.getenv("JOB_EXPORT_PATH", "/data/replays/exports")).resolve()
COURT_WIDTH = 800
COURT_HEIGHT = 480
PADDLE_HEIGHT = 80
PADDLE_WIDTH = 12
BALL_SIZE = 12
RENDER_WIDTH = 1280
RENDER_HEIGHT = 720
FRAME_RATE = 20
FRAME_INTERVAL_MS = int(1000 / FRAME_RATE)
# Pillow를 선택한 이유: 도형 기반 렌더링만 필요해 외부 엔진 없이도 GameCanvas와 비슷한 형태를 유지할 수 있다.
# 렌더링 해상도는 1280x720 고정이며, 게임 월드(800x480)를 중앙 정렬하고 비율이 깨지지 않도록 최소 배율로 스케일링한다.

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("replay-worker")

r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)


def detect_ffmpeg_capabilities() -> dict:
    """ffmpeg이 제공하는 하드웨어 가속 관련 정보를 수집한다."""
    result = {"encoders": [], "hwaccels": [], "decoders": []}
    try:
        enc_output = subprocess.check_output(["ffmpeg", "-hide_banner", "-encoders"], text=True, errors="ignore")
        result["encoders"] = [
            line.split()[1]
            for line in enc_output.splitlines()
            if any(tag in line for tag in ("nvenc", "qsv", "vaapi", "videotoolbox", "amf")) and len(line.split()) > 1
        ]
    except Exception as exc:  # noqa: BLE001
        logger.warning("FFmpeg 인코더 목록 조회 실패: %s", exc)

    try:
        hw_output = subprocess.check_output(["ffmpeg", "-hide_banner", "-hwaccels"], text=True, errors="ignore")
        result["hwaccels"] = [line.strip() for line in hw_output.splitlines() if line.strip() and not line.startswith("Hardware")]
    except Exception as exc:  # noqa: BLE001
        logger.warning("FFmpeg hwaccel 목록 조회 실패: %s", exc)

    try:
        dec_output = subprocess.check_output(["ffmpeg", "-hide_banner", "-decoders"], text=True, errors="ignore")
        result["decoders"] = [
            line.split()[1]
            for line in dec_output.splitlines()
            if any(tag in line for tag in ("h264", "hevc")) and len(line.split()) > 1
        ]
    except Exception as exc:  # noqa: BLE001
        logger.warning("FFmpeg 디코더 목록 조회 실패: %s", exc)
    return result


def select_hw_encoder(capabilities: dict) -> Optional[str]:
    """사용 가능한 하드웨어 인코더를 우선순위대로 선택한다."""
    preferred = ["h264_nvenc", "h264_qsv", "h264_vaapi", "h264_videotoolbox", "h264_amf"]
    for encoder in preferred:
        if encoder in capabilities.get("encoders", []):
            return encoder
    return None


def is_hw_enabled() -> bool:
    return os.getenv("EXPORT_HW_ACCEL", "false").lower() == "true"


FFMPEG_CAPABILITIES = detect_ffmpeg_capabilities()
SELECTED_HW_ENCODER = select_hw_encoder(FFMPEG_CAPABILITIES)
if SELECTED_HW_ENCODER:
    logger.info(
        "하드웨어 인코딩 감지: encoder=%s, hwaccel=%s", SELECTED_HW_ENCODER, ",".join(FFMPEG_CAPABILITIES.get("hwaccels", []))
    )
else:
    logger.info("하드웨어 인코딩 인식 실패 또는 미지원: CPU(libx264)로 폴백 예정")


class ReplayFormatError(Exception):
    """JSONL 구조가 잘못되었을 때 사용하는 예외."""


class OutputPathError(Exception):
    """허용된 출력 루트 밖을 요청했을 때 사용하는 예외."""


@dataclass
class Snapshot:
    ball_x: float
    ball_y: float
    left_paddle_y: float
    right_paddle_y: float
    left_score: int
    right_score: int
    target_score: int
    finished: bool


@dataclass
class ReplayEvent:
    offset_ms: int
    snapshot: Snapshot


def ensure_group() -> None:
    try:
        r.xgroup_create(REQUEST_STREAM, CONSUMER_GROUP, id="0", mkstream=True)
    except redis.ResponseError as exc:  # noqa: PERF203
        if "BUSYGROUP" not in str(exc):
            raise


def recover_pending() -> None:
    """워커 재시작 시 대기 중인 pending 메시지를 다시 가져온다."""
    start_id = "0-0"
    while True:
        try:
            claimed = r.xautoclaim(REQUEST_STREAM, CONSUMER_GROUP, CONSUMER_NAME,
                                   min_idle_time=0, start_id=start_id, count=10)
        except redis.ResponseError:
            fallback = r.xreadgroup(CONSUMER_GROUP, CONSUMER_NAME, {REQUEST_STREAM: "0"}, count=10)
            if not fallback:
                break
            for _, items in fallback:
                for message_id, fields in items:
                    process_request(fields)
                    r.xack(REQUEST_STREAM, CONSUMER_GROUP, message_id)
            break
        next_start = start_id
        messages = []
        if isinstance(claimed, (list, tuple)):
            if len(claimed) >= 1:
                next_start = claimed[0]
            if len(claimed) >= 2 and isinstance(claimed[1], list):
                messages = claimed[1]
        if not messages:
            break
        for message_id, fields in messages:
            process_request(fields)
            r.xack(REQUEST_STREAM, CONSUMER_GROUP, message_id)
        start_id = next_start


def publish_progress(job_id: str, progress: int, phase: str, message: str) -> None:
    r.xadd(PROGRESS_STREAM, {
        "jobId": job_id,
        "progress": str(progress),
        "phase": phase,
        "message": message,
    })


def publish_result(job_id: str, status: str, result_uri: str = "", checksum: str = "",
                   error_code: str = "", error_message: str = "") -> None:
    r.xadd(RESULT_STREAM, {
        "jobId": job_id,
        "status": status,
        "resultUri": result_uri,
        "checksum": checksum,
        "errorCode": error_code,
        "errorMessage": error_message,
    })


def checksum_file(path: Path) -> str:
    import hashlib

    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def safe_int(value, default: int) -> int:
    try:
        return int(value)
    except Exception:  # noqa: BLE001
        return default


def safe_float(value, default: float) -> float:
    try:
        return float(value)
    except Exception:  # noqa: BLE001
        return default


def validate_output_path(raw_path: str) -> Path:
    if not raw_path:
        raise OutputPathError("outputPath가 비어 있습니다.")
    resolved = Path(raw_path).expanduser().resolve()
    try:
        if not resolved.is_relative_to(EXPORT_ROOT):
            raise OutputPathError("허용된 출력 루트가 아닙니다.")
    except AttributeError:
        if EXPORT_ROOT not in resolved.parents and resolved != EXPORT_ROOT:
            raise OutputPathError("허용된 출력 루트가 아닙니다.")
    resolved.parent.mkdir(parents=True, exist_ok=True)
    return resolved


def load_events(input_path: str) -> List[ReplayEvent]:
    if not input_path:
        raise ReplayFormatError("inputPath가 없습니다.")
    path = Path(input_path)
    if not path.exists():
        raise ReplayFormatError("입력 JSONL을 찾을 수 없습니다.")
    events: List[ReplayEvent] = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            try:
                data = json.loads(line)
            except json.JSONDecodeError as exc:  # noqa: TRY003
                raise ReplayFormatError("JSONL 파싱에 실패했습니다.") from exc
            raw_snapshot = data.get("snapshot")
            if raw_snapshot is None:
                raise ReplayFormatError("snapshot이 없습니다.")
            snapshot = Snapshot(
                ball_x=safe_float(raw_snapshot.get("ballX"), COURT_WIDTH / 2),
                ball_y=safe_float(raw_snapshot.get("ballY"), COURT_HEIGHT / 2),
                left_paddle_y=safe_float(raw_snapshot.get("leftPaddleY"), (COURT_HEIGHT - PADDLE_HEIGHT) / 2),
                right_paddle_y=safe_float(raw_snapshot.get("rightPaddleY"), (COURT_HEIGHT - PADDLE_HEIGHT) / 2),
                left_score=safe_int(raw_snapshot.get("leftScore"), 0),
                right_score=safe_int(raw_snapshot.get("rightScore"), 0),
                target_score=safe_int(raw_snapshot.get("targetScore"), 5),
                finished=bool(raw_snapshot.get("finished", False)),
            )
            events.append(ReplayEvent(offset_ms=safe_int(data.get("offsetMs", 0), 0), snapshot=snapshot))
    if not events:
        raise ReplayFormatError("리플레이 이벤트가 비어 있습니다.")
    return events


def frame_count(total_ms: int) -> int:
    return max(1, int((total_ms + FRAME_INTERVAL_MS - 1) / FRAME_INTERVAL_MS))


def to_pixels(value: float, scale: float, offset: float) -> float:
    return value * scale + offset


def render_frame(snapshot: Snapshot) -> Image.Image:
    scale = min(RENDER_WIDTH / COURT_WIDTH, RENDER_HEIGHT / COURT_HEIGHT)
    offset_x = (RENDER_WIDTH - COURT_WIDTH * scale) / 2
    offset_y = (RENDER_HEIGHT - COURT_HEIGHT * scale) / 2

    image = Image.new("RGB", (RENDER_WIDTH, RENDER_HEIGHT), (12, 18, 28))
    draw = ImageDraw.Draw(image)
    center_x = RENDER_WIDTH // 2
    draw.line([(center_x, 0), (center_x, RENDER_HEIGHT)], fill=(60, 70, 85), width=4)

    paddle_height_px = PADDLE_HEIGHT * scale
    left_paddle_top = to_pixels(snapshot.left_paddle_y, scale, offset_y)
    right_paddle_top = to_pixels(snapshot.right_paddle_y, scale, offset_y)
    draw.rectangle([
        (offset_x + 24, left_paddle_top),
        (offset_x + 24 + PADDLE_WIDTH, left_paddle_top + paddle_height_px),
    ], fill=(230, 230, 230))
    draw.rectangle([
        (RENDER_WIDTH - offset_x - 24 - PADDLE_WIDTH, right_paddle_top),
        (RENDER_WIDTH - offset_x - 24, right_paddle_top + paddle_height_px),
    ], fill=(230, 230, 230))

    ball_left = to_pixels(snapshot.ball_x, scale, offset_x) - (BALL_SIZE / 2)
    ball_top = to_pixels(snapshot.ball_y, scale, offset_y) - (BALL_SIZE / 2)
    draw.ellipse([
        (ball_left, ball_top),
        (ball_left + BALL_SIZE, ball_top + BALL_SIZE),
    ], fill=(255, 180, 90))

    font = ImageFont.load_default()
    score_text = f"{snapshot.left_score} : {snapshot.right_score} (목표 {snapshot.target_score})"
    draw.text((10, 10), score_text, font=font, fill=(240, 240, 240))
    if snapshot.finished:
        draw.text((RENDER_WIDTH / 2 - 40, 20), "FINISH", font=font, fill=(200, 255, 200))
    return image


def generate_frame_bytes(events: List[ReplayEvent], total_ms: int) -> Iterable[bytes]:
    total_frames = frame_count(total_ms)
    current_index = 0
    for frame_no in range(total_frames):
        target_ms = frame_no * FRAME_INTERVAL_MS
        while current_index + 1 < len(events) and events[current_index + 1].offset_ms <= target_ms:
            current_index += 1
        yield render_frame(events[current_index].snapshot).tobytes()


def is_valid_mp4(path: Path) -> bool:
    if not path.exists() or path.stat().st_size == 0:
        return False
    with path.open("rb") as f:
        head = f.read(256)
    return b"ftyp" in head


def is_valid_png(path: Path) -> bool:
    if not path.exists() or path.stat().st_size == 0:
        return False
    with path.open("rb") as f:
        head = f.read(8)
    return head.startswith(b"\x89PNG\r\n\x1a\n")


def calculate_expected_ms(events: List[ReplayEvent], duration_ms: int) -> int:
    last_offset = events[-1].offset_ms if events else 0
    base = max(duration_ms, last_offset)
    if base <= 0:
        return max(len(events) * FRAME_INTERVAL_MS, FRAME_INTERVAL_MS)
    return base


def run_ffmpeg(job_id: str, output_path: Path, frames: Iterable[bytes], expected_ms: int,
               phase: str, progress_cb: Callable[[str, int, str, str], None], encoder: str,
               hwaccel: Optional[str] = None, filters: Optional[List[str]] = None) -> None:
    command = [
        "ffmpeg", "-y",
        "-f", "rawvideo", "-pix_fmt", "rgb24",
        "-s", f"{RENDER_WIDTH}x{RENDER_HEIGHT}",
        "-r", str(FRAME_RATE),
        "-i", "pipe:0",
        "-c:v", encoder,
        "-pix_fmt", "yuv420p",
        "-movflags", "+faststart",
        "-f", "mp4",
        "-progress", "pipe:1",
        "-loglevel", "error",
        str(output_path),
    ]
    if hwaccel:
        command = ["ffmpeg", "-y", "-hwaccel", hwaccel, "-f", "rawvideo", "-pix_fmt", "rgb24",
                   "-s", f"{RENDER_WIDTH}x{RENDER_HEIGHT}", "-r", str(FRAME_RATE), "-i", "pipe:0",
                   "-c:v", encoder, "-pix_fmt", "yuv420p"]
        if filters:
            command.extend(["-vf", ",".join(filters)])
        command.extend(["-movflags", "+faststart", "-f", "mp4", "-progress", "pipe:1", "-loglevel", "error", str(output_path)])
    progress_cb(job_id, 5, "PREPARE", "ffmpeg 준비 중")
    process = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=False)

    def consume_progress() -> None:
        if process.stdout is None:
            return
        for raw_line in process.stdout:
            line = raw_line.decode(errors="ignore").strip()
            if line.startswith("out_time_ms") and expected_ms > 0:
                try:
                    out_us = int(line.split("=")[1])
                    percent = int(out_us / (expected_ms * 1000) * 100)
                    percent = max(0, min(99, percent))
                    progress_cb(job_id, percent, phase, "인코딩 진행 중")
                except Exception:  # noqa: BLE001
                    continue
            if line.startswith("progress=end"):
                progress_cb(job_id, 99, phase, "마무리 중")

    progress_thread = threading.Thread(target=consume_progress, daemon=True)
    progress_thread.start()
    try:
        if process.stdin is None:
            raise RuntimeError("ffmpeg stdin을 열 수 없습니다.")
        for frame in frames:
            process.stdin.write(frame)
        process.stdin.close()
    except Exception as exc:  # noqa: BLE001
        stderr_output = ""
        if process.stderr:
            try:
                stderr_output = process.stderr.read().decode(errors="ignore")
            except Exception:  # noqa: BLE001
                stderr_output = ""
        process.kill()
        process.wait()
        progress_thread.join(timeout=1)
        raise RuntimeError(f"ffmpeg 쓰기 실패: {exc}; stderr={stderr_output.strip()}") from exc
    finally:
        process.wait()
        progress_thread.join(timeout=1)
    if process.returncode != 0:
        stderr_output = process.stderr.read() if process.stderr else ""
        raise RuntimeError(f"ffmpeg 실패: {stderr_output.strip()}")
    progress_cb(job_id, 100, phase, "완료")
    if process.stdout:
        process.stdout.close()
    if process.stderr:
        process.stderr.close()


def export_mp4(job_id: str, replay_id: str, options: dict,
               progress_cb: Callable[[str, int, str, str], None] = publish_progress,
               result_cb: Callable[..., None] = publish_result,
               ffmpeg_runner: Callable[..., None] = run_ffmpeg) -> None:
    try:
        output_path = validate_output_path(options.get("outputPath"))
    except OutputPathError as exc:
        result_cb(job_id, "FAILED", error_code="INVALID_OUTPUT_PATH", error_message=str(exc))
        return

    input_path = options.get("inputPath")
    duration_ms = safe_int(options.get("durationMs", 0), 0)
    try:
        events = load_events(input_path)
    except ReplayFormatError as exc:
        result_cb(job_id, "FAILED", error_code="INVALID_REPLAY_FORMAT", error_message=str(exc))
        return

    if output_path.exists() and is_valid_mp4(output_path):
        checksum = checksum_file(output_path)
        result_cb(job_id, "SUCCEEDED", str(output_path), checksum)
        return
    if output_path.exists():
        output_path.unlink(missing_ok=True)

    expected_ms = calculate_expected_ms(events, duration_ms)
    tmp_path = output_path.with_name(output_path.name + ".tmp")
    tmp_path.unlink(missing_ok=True)

    def run_encode(encoder: str, hwaccel: Optional[str] = None, filters: Optional[List[str]] = None) -> None:
        frames = generate_frame_bytes(events, expected_ms)
        ffmpeg_runner(job_id, tmp_path, frames, expected_ms, "ENCODE", progress_cb, encoder, hwaccel, filters)

    hw_attempted = False
    if is_hw_enabled() and SELECTED_HW_ENCODER:
        hw_attempted = True
        filters: Optional[List[str]] = None
        hwaccel = None
        if SELECTED_HW_ENCODER == "h264_vaapi":
            hwaccel = "vaapi"
            filters = ["format=nv12", "hwupload"]
        elif SELECTED_HW_ENCODER == "h264_qsv":
            hwaccel = "qsv"
        elif SELECTED_HW_ENCODER == "h264_nvenc":
            hwaccel = "cuda"
        logger.info("하드웨어 인코딩 시도: encoder=%s, hwaccel=%s", SELECTED_HW_ENCODER, hwaccel or "none")
        try:
            run_encode(SELECTED_HW_ENCODER, hwaccel, filters)
        except Exception as exc:  # noqa: BLE001
            logger.warning("하드웨어 인코딩 실패, CPU 경로로 폴백: %s", exc)
            tmp_path.unlink(missing_ok=True)

    try:
        if not tmp_path.exists():
            run_encode("libx264")
        tmp_path.replace(output_path)
        checksum = checksum_file(output_path)
        result_cb(job_id, "SUCCEEDED", str(output_path), checksum)
    except RuntimeError as exc:
        tmp_path.unlink(missing_ok=True)
        output_path.unlink(missing_ok=True)
        code = "FFMPEG_FAILED_HW" if hw_attempted else "FFMPEG_FAILED"
        result_cb(job_id, "FAILED", error_code=code, error_message=str(exc))
    except Exception as exc:  # noqa: BLE001
        tmp_path.unlink(missing_ok=True)
        output_path.unlink(missing_ok=True)
        code = "WORKER_ERROR"
        if hw_attempted:
            code = "WORKER_HW_FALLBACK_FAILED"
        result_cb(job_id, "FAILED", error_code=code, error_message=str(exc))


def export_thumbnail(job_id: str, replay_id: str, options: dict,
                     progress_cb: Callable[[str, int, str, str], None] = publish_progress,
                     result_cb: Callable[..., None] = publish_result) -> None:
    try:
        output_path = validate_output_path(options.get("outputPath"))
    except OutputPathError as exc:
        result_cb(job_id, "FAILED", error_code="INVALID_OUTPUT_PATH", error_message=str(exc))
        return

    input_path = options.get("inputPath")
    try:
        events = load_events(input_path)
    except ReplayFormatError as exc:
        result_cb(job_id, "FAILED", error_code="INVALID_REPLAY_FORMAT", error_message=str(exc))
        return

    if output_path.exists() and is_valid_png(output_path):
        checksum = checksum_file(output_path)
        result_cb(job_id, "SUCCEEDED", str(output_path), checksum)
        return
    if output_path.exists():
        output_path.unlink(missing_ok=True)

    pivot_index = len(events) // 2
    frame = render_frame(events[pivot_index].snapshot)
    tmp_path = output_path.with_name(output_path.name + ".tmp")
    tmp_path.unlink(missing_ok=True)
    try:
        frame.save(tmp_path, format="PNG")
        tmp_path.replace(output_path)
        checksum = checksum_file(output_path)
        progress_cb(job_id, 100, "THUMBNAIL", "완료")
        result_cb(job_id, "SUCCEEDED", str(output_path), checksum)
    except Exception as exc:  # noqa: BLE001
        tmp_path.unlink(missing_ok=True)
        output_path.unlink(missing_ok=True)
        result_cb(job_id, "FAILED", error_code="WORKER_ERROR", error_message=str(exc))


def process_request(fields: dict) -> None:
    job_id = fields.get("jobId")
    job_type = fields.get("jobType")
    replay_id = fields.get("replayId", "")
    options = {k: v for k, v in fields.items() if k not in {"jobId", "jobType", "replayId"}}
    if not job_id or not job_type:
        return
    try:
        publish_progress(job_id, 10, "QUEUE", "워커가 작업을 시작했습니다")
        if job_type == "REPLAY_EXPORT_MP4":
            export_mp4(job_id, replay_id, options)
        elif job_type == "REPLAY_THUMBNAIL":
            export_thumbnail(job_id, replay_id, options)
        else:
            publish_result(job_id, "FAILED", error_code="UNSUPPORTED_TYPE",
                           error_message=f"지원하지 않는 유형 {job_type}")
    except Exception as exc:  # noqa: BLE001
        publish_result(job_id, "FAILED", error_code="WORKER_ERROR", error_message=str(exc))


def main_loop() -> None:
    EXPORT_ROOT.mkdir(parents=True, exist_ok=True)
    ensure_group()
    recover_pending()
    while True:
        messages = r.xreadgroup(CONSUMER_GROUP, CONSUMER_NAME, {REQUEST_STREAM: ">"}, count=1, block=5000)
        if not messages:
            recover_pending()
            continue
        for _, items in messages:
            for message_id, fields in items:
                process_request(fields)
                r.xack(REQUEST_STREAM, CONSUMER_GROUP, message_id)
                time.sleep(0.1)


if __name__ == "__main__":
    main_loop()
