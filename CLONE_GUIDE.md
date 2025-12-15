# CLONE_GUIDE (v1.0.0)

## 1. 목적
- v1.0.0 포트폴리오 공개용 기준으로 전체 스택을 클론→실행→테스트→수동 점검하는 절차를 정리한다.
- 회원가입→로그인→게임→전적/랭킹, 친구/채팅/토너먼트/관전, 리플레이/잡 확인까지 **실사용 흐름**을 재현할 수 있게 한다.

## 2. 사전 준비물
- Git
- Docker / Docker Compose (데몬 권한 필요)
- Node.js 18 (프런트엔드 로컬 실행 시)
- JDK 17 (백엔드 로컬 실행 시)
- ffmpeg CLI (워커 단독 실행 시 필요)
- Gradle 8+ 로컬 설치: 저장소에는 `gradle-wrapper.jar`가 없으므로 시스템 Gradle을 사용하거나, 임시로 wrapper를 내려받아 실행 후 삭제한다.

## 3. 클론 및 기본 구조
```bash
git clone <repo-url>
cd codex-pong
```
- 주요 디렉터리
  - `backend/`: Spring Boot 소스
  - `frontend/`: React + Vite 소스
  - `worker/`: Redis Streams 기반 replay-worker
  - `infra/`: Nginx/모니터링 설정
  - `design/`: 한국어 설계 문서

## 4. 환경변수
- 백엔드 (docker-compose 기본값)
  - `DB_HOST=db`, `DB_NAME=codexpong`, `DB_USER=codexpong`, `DB_PASSWORD=codexpong`
  - `AUTH_JWT_SECRET` (32바이트 이상, 기본 `change-me-in-prod-secret-please-keep-long`)
  - `AUTH_JWT_EXPIRATION_SECONDS` (기본 3600)
  - `AUTH_KAKAO_PROFILE_URI` (기본 `https://kapi.kakao.com/v2/user/me`, 모킹 시 오버라이드)
  - `AUTH_NAVER_PROFILE_URI` (기본 `https://openapi.naver.com/v1/nid/me`)
  - `REPLAY_STORAGE_PATH` (기본 `${user.dir}/build/replays`, Compose는 `/data/replays` 네임드 볼륨)
  - `REPLAY_RETENTION_MAX_PER_USER` (기본 20)
  - `JOB_EXPORT_PATH` (기본 `${REPLAY_STORAGE_PATH}/exports`)
  - `REDIS_HOST=redis`, `REDIS_PORT=6379`
  - 잡 큐: `JOB_QUEUE_ENABLED=true`, `JOB_QUEUE_REQUEST_STREAM=job.requests`, `JOB_QUEUE_PROGRESS_STREAM=job.progress`, `JOB_QUEUE_RESULT_STREAM=job.results`, `JOB_QUEUE_CONSUMER_GROUP=replay-jobs`
- 프런트엔드
  - `VITE_BACKEND_URL` (기본 `http://localhost:8080`)
  - `VITE_BACKEND_WS` (기본 `ws://localhost:8080`)
- 워커(로컬 단독 실행 시)
  - `REDIS_HOST`/`REDIS_PORT`
  - `JOB_QUEUE_REQUEST_STREAM`/`JOB_QUEUE_PROGRESS_STREAM`/`JOB_QUEUE_RESULT_STREAM`/`JOB_QUEUE_CONSUMER_GROUP`
  - `WORKER_ID` (로그 구분용)
  - `EXPORT_HW_ACCEL` (선택, true면 GPU 인코더 우선 시도 후 CPU 폴백)
- 기본 `TZ=Asia/Seoul`, DB 콜레이션 `utf8mb4_unicode_ci` 유지.

## 5. Docker Compose 실행
```bash
docker compose build --progress=plain
docker compose up -d
```
- 서비스
  - `backend`, `frontend`, `db`(MariaDB), `redis`, `replay-worker`, `prometheus`, `grafana`, `nginx`
- 접속 경로
  - 웹: http://localhost/
  - 헬스체크: http://localhost/api/health
  - 로그인 후
    - 리더보드: http://localhost/leaderboard
    - 리플레이 목록: http://localhost/replays
    - 작업 목록: http://localhost/jobs
    - 관리자 콘솔: http://localhost/admin
    - 관전 목록: http://localhost/spectate
  - WebSocket 예시: ws://localhost/ws/game?roomId=<room>&token=<JWT>

## 6. 개별 서비스 로컬 실행 (선택)
### 6.1 백엔드
```bash
cd backend
gradle bootRun
```
- Redis/DB가 로컬에서 접근 가능해야 하며, 잡 소비를 끄려면 `JOB_QUEUE_ENABLED=false` 설정.

### 6.2 프런트엔드
```bash
cd frontend
npm install
npm run dev -- --host --port 5173
```

### 6.3 워커 (Docker 없이 로컬 실행)
```bash
cd worker
pip install -r requirements.txt
export REDIS_HOST=localhost
export JOB_QUEUE_REQUEST_STREAM=job.requests
export JOB_QUEUE_PROGRESS_STREAM=job.progress
export JOB_QUEUE_RESULT_STREAM=job.results
export JOB_QUEUE_CONSUMER_GROUP=replay-jobs
export WORKER_ID=local-worker
python main.py
```
- ffmpeg/ffprobe가 PATH에 있어야 하며, 입력/출력 경로는 백엔드와 동일하게 맞춘다.

## 7. 테스트 실행
### 7.1 백엔드 테스트
```bash
cd backend
gradle test
```
- Testcontainers로 Redis를 기동해 `JobFlowTest` 등 통합 테스트를 포함한다.

### 7.2 프런트엔드 테스트/빌드
```bash
cd frontend
npm install
npm test
npm run build
```

### 7.3 워커 테스트
```bash
pip install -r worker/requirements.txt
REQUIRE_FFMPEG=1 python -m unittest discover -s worker -p "test_*.py"
```

## 8. 수동 점검 체크리스트
- 회원가입→로그인→프로필 수정→로그아웃까지 REST 흐름 확인.
- 로비에서 NORMAL/RANKED 매칭 → 게임 종료 후 리더보드/전적 반영 확인.
- 친구 추가/수락/차단 → 소셜 알림 WebSocket 수신 → 채팅방 메시지 송수신/금칙어 필터 확인.
- 토너먼트 생성/참여/시작 → 브래킷 업데이트 → 각 매치 roomId로 게임 진입 → 관전 세션 열기.
- 리플레이 녹화/익스포트 요청 → `jobs` 목록에서 진행률/실패 메시지 확인 → `replays`에서 시청.
- 관리자 페이지에서 특정 사용자 상태 변경(BAN/MUTE) 후 채팅/매칭 차단 여부 확인.

## 9. 버전 메모 (v1.0.0)
- 포트폴리오 공개용으로 주요 기능이 안정화되었으며, README 대신 본 가이드에서 실행/점검 절차를 제공한다.
- OAuth 로그인은 액세스 토큰 입력 기반 데모 흐름이며, 실제 배포 시 리다이렉트/동의 화면을 갖춘 인가 서버 설정이 필요하다.
- 단일 인스턴스/단일 Redis 전제로 구성되어 있어, 멀티 노드 확장 시 세션 스티키니스/동기화 계층 추가가 필요하다.
