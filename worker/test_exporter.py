import json
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

import worker.main as worker


class ExporterTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        worker.EXPORT_ROOT = Path(self.temp_dir.name)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def _write_sample_replay(self) -> Path:
        events = [
            {
                "offsetMs": 0,
                "snapshot": {
                    "ballX": 400,
                    "ballY": 240,
                    "leftPaddleY": 180,
                    "rightPaddleY": 200,
                    "leftScore": 0,
                    "rightScore": 0,
                    "targetScore": 5,
                    "finished": False,
                },
            },
            {
                "offsetMs": 500,
                "snapshot": {
                    "ballX": 500,
                    "ballY": 260,
                    "leftPaddleY": 220,
                    "rightPaddleY": 240,
                    "leftScore": 1,
                    "rightScore": 0,
                    "targetScore": 5,
                    "finished": False,
                },
            },
        ]
        input_path = Path(self.temp_dir.name) / "sample.jsonl"
        with input_path.open("w", encoding="utf-8") as f:
            for event in events:
                f.write(json.dumps(event))
                f.write("\n")
        return input_path

    def _require_ffmpeg(self) -> None:
        ffmpeg_path = shutil.which("ffmpeg")
        ffprobe_path = shutil.which("ffprobe")
        if ffmpeg_path and ffprobe_path:
            return
        if os.getenv("REQUIRE_FFMPEG") == "1" or os.getenv("CI"):
            self.fail("CI에서는 ffmpeg/ffprobe가 필수입니다.")
        self.skipTest("ffmpeg/ffprobe가 없어 로컬 테스트를 건너뜁니다.")

    def test_export_mp4_generates_video(self) -> None:
        self._require_ffmpeg()
        input_path = self._write_sample_replay()
        output_path = Path(self.temp_dir.name) / "exports" / "video.mp4"
        result_logs: list = []

        worker.export_mp4(
            "job-1",
            "replay-1",
            {
                "inputPath": str(input_path),
                "outputPath": str(output_path),
                "durationMs": "1000",
            },
            progress_cb=lambda *_args, **_kwargs: None,
            result_cb=lambda *args, **_kwargs: result_logs.append(args),
        )

        self.assertTrue(output_path.exists(), "mp4가 생성되어야 합니다.")
        self.assertGreater(output_path.stat().st_size, 0, "mp4 크기가 0이면 안 됩니다.")
        with output_path.open("rb") as f:
            header = f.read(256)
        self.assertIn(b"ftyp", header, "MP4 ftyp 헤더가 포함되어야 합니다.")

        probe_output = subprocess.check_output([
            "ffprobe",
            "-v",
            "error",
            "-show_entries",
            "format=duration",
            "-show_streams",
            "-of",
            "json",
            str(output_path),
        ])
        probe_json = json.loads(probe_output.decode("utf-8"))
        video_streams = [s for s in probe_json.get("streams", []) if s.get("codec_type") == "video"]
        self.assertTrue(video_streams, "비디오 스트림이 포함되어야 합니다.")
        video_stream = video_streams[0]
        self.assertEqual(video_stream.get("codec_name"), "h264", "h264 코덱으로 인코딩되어야 합니다.")
        self.assertEqual(video_stream.get("width"), worker.RENDER_WIDTH, "가로 해상도가 맞아야 합니다.")
        self.assertEqual(video_stream.get("height"), worker.RENDER_HEIGHT, "세로 해상도가 맞아야 합니다.")
        fps_raw = video_stream.get("r_frame_rate") or video_stream.get("avg_frame_rate")
        if fps_raw and fps_raw != "0/0":
            num, denom = fps_raw.split("/")
            fps_value = float(num) / float(denom)
            self.assertGreaterEqual(fps_value, worker.FRAME_RATE - 0.5, "프레임레이트가 예상치보다 낮습니다.")
        self.assertTrue(any(log for log in result_logs if "SUCCEEDED" in log), "완료 로그가 있어야 합니다.")

    def test_export_thumbnail_generates_png(self) -> None:
        input_path = self._write_sample_replay()
        output_path = Path(self.temp_dir.name) / "exports" / "thumb.png"
        worker.export_thumbnail(
            "job-2",
            "replay-2",
            {
                "inputPath": str(input_path),
                "outputPath": str(output_path),
            },
            progress_cb=lambda *_args, **_kwargs: None,
            result_cb=lambda *_args, **_kwargs: None,
        )

        self.assertTrue(output_path.exists(), "PNG가 생성되어야 합니다.")
        self.assertGreater(output_path.stat().st_size, 0, "PNG 크기가 0이면 안 됩니다.")
        with output_path.open("rb") as f:
            header = f.read(8)
        self.assertEqual(header, b"\x89PNG\r\n\x1a\n", "PNG 시그니처가 일치해야 합니다.")

    def test_hw_encode_flag_falls_back_to_software(self) -> None:
        input_path = self._write_sample_replay()
        output_path = Path(self.temp_dir.name) / "exports" / "video.mp4"
        result_logs: list = []
        os.environ["EXPORT_HW_ACCEL"] = "true"
        original_encoder = worker.SELECTED_HW_ENCODER
        worker.SELECTED_HW_ENCODER = "h264_nvenc"

        def fake_runner(job_id, target_path, frames, expected_ms, phase, progress_cb, encoder, hwaccel=None, filters=None):
            # 첫 번째 호출(h264_nvenc)은 실패시키고, 두 번째 호출(libx264)은 성공 파일을 쓴다.
            if encoder != "libx264":
                raise RuntimeError("GPU 경로 실패")
            target_path.parent.mkdir(parents=True, exist_ok=True)
            target_path.write_bytes(b"ftypfallback")

        try:
            worker.export_mp4(
                "job-hw-fallback",
                "replay-x",
                {
                    "inputPath": str(input_path),
                    "outputPath": str(output_path),
                    "durationMs": "1000",
                },
                progress_cb=lambda *_args, **_kwargs: None,
                result_cb=lambda *args, **_kwargs: result_logs.append(args),
                ffmpeg_runner=fake_runner,
            )
        finally:
            os.environ.pop("EXPORT_HW_ACCEL", None)
            worker.SELECTED_HW_ENCODER = original_encoder

        self.assertTrue(output_path.exists(), "HW 실패 후 CPU 폴백으로 파일을 남겨야 한다.")
        self.assertTrue(any(log for log in result_logs if "SUCCEEDED" in log), "성공 로그가 기록돼야 한다.")


if __name__ == "__main__":
    unittest.main()
