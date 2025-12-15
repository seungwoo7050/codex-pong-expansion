# DOC_TEMPLATES.md

This document provides **templates and examples** for:

- Source code comments (backend, frontend, realtime, infra)
- `design/` documents (backend / frontend / realtime / infra)
- `CLONE_GUIDE.md`
- Consistency rules between code, design docs, and clone guides

> This file is in **English** for AI/tooling.  
> **All comments in code and all other documentation MUST be written in Korean.**

---

## 1. Code comment templates (Korean-only)

### 1.1 Global rules

When writing or modifying code, you MUST:

- Write all comments in **Korean**.
- Add a **file/module header comment** for important files:
  - File path / module name
  - Short description
  - Version (e.g. `v0.1.0`)
  - Related design document path(s)
  - Short change history
- Add a **function/method/class comment** for public or important APIs:
  - Purpose
  - Inputs / outputs
  - Errors / exceptions
  - Related tests
  - Related design doc

Do NOT repeat code literally; explain **intent, constraints, and behavior**.

---

### 1.2 Backend example (Java/Kotlin file header, in Korean)

```java
/**
 * [모듈] backend/src/main/java/com/example/transcendence/auth/AuthController.java
 * 설명:
 *   - 인증 관련 HTTP 엔드포인트를 제공한다.
 *   - 로그인, 로그아웃, 토큰 갱신 등의 기능을 담당한다.
 * 버전: v0.1.0
 * 관련 설계문서:
 *   - design/backend/v0.1.0-auth-and-core-game.md
 * 변경 이력:
 *   - v0.1.0: 기본 로그인/로그아웃 API 추가
 * 테스트:
 *   - backend/src/test/java/com/example/transcendence/auth/AuthControllerTest.java
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // ...
}
```

---

### 1.3 Backend example (method-level, in Korean)

```java
/**
 * login
 * 설명:
 *   - 사용자의 아이디/비밀번호를 검증하고, 유효한 세션 또는 토큰을 발급한다.
 * 입력:
 *   - request: 로그인 요청 정보 (아이디, 비밀번호)
 * 출력:
 *   - ResponseEntity<AuthResponse>: 인증 성공 시 토큰/세션 정보 포함
 * 에러:
 *   - 인증 실패 시 401 상태 코드와 오류 코드를 반환한다.
 * 관련 설계문서:
 *   - design/backend/v0.1.0-auth-and-core-game.md
 * 관련 테스트:
 *   - AuthControllerTest.login_success
 *   - AuthControllerTest.login_invalid_password
 */
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    // ...
}
```

---

### 1.4 Frontend example (component header, in Korean)

```tsx
/**
 * [컴포넌트] frontend/src/features/lobby/LobbyPage.tsx
 * 설명:
 *   - 매칭 대기실 화면을 렌더링한다.
 *   - 현재 대기 중인 매치 정보와 빠른 시작 버튼을 제공한다.
 * 버전: v0.1.0
 * 관련 설계문서:
 *   - design/frontend/v0.1.0-lobby-and-game-ui.md
 * 변경 이력:
 *   - v0.1.0: 기본 레이아웃과 매칭 버튼 추가
 */
export function LobbyPage() {
  // ...
}
```

---

### 1.5 Frontend example (hook-level, in Korean)

```tsx
/**
 * useMatchmaking
 * 설명:
 *   - 매칭 요청/취소, 매칭 상태 조회를 담당하는 훅이다.
 * 입력:
 *   - 없음 (내부에서 현재 사용자 정보를 사용한다)
 * 출력:
 *   - 상태값과 핸들러 (isMatching, startMatch, cancelMatch 등)를 반환한다.
 * 에러:
 *   - 서버 통신 실패 시 토스트/알림을 통해 사용자에게 오류를 알려야 한다.
 * 관련 설계문서:
 *   - design/frontend/v0.1.0-lobby-and-game-ui.md
 * 관련 테스트:
 *   - useMatchmaking.test.tsx
 */
export function useMatchmaking() {
  // ...
}
```

---

### 1.6 Realtime example (game engine, in Korean)

```ts
/**
 * GameEngine (v0.1.0)
 * 설명:
 *   - 한 판의 게임에 대한 상태와 틱 기반 업데이트 로직을 관리한다.
 *   - 공 위치, 패들 위치, 점수 등 핵심 게임 상태를 담당한다.
 * 설계:
 *   - design/realtime/v0.1.0-game-loop-and-events.md
 * 주의 사항:
 *   - 틱 순서(입력 적용 → 물리 계산 → 충돌 처리 → 점수 체크)를 반드시 유지한다.
 *   - 재현 가능성을 위해 랜덤 요소가 있다면 시드 관리 방식을 설계 문서에 명시한다.
 */
export class GameEngine {
  // ...
}
```

---

### 1.7 Infra example (Nginx config comment, in Korean)

```nginx
# /infra/nginx/nginx.conf
# 설명:
#   - 프론트엔드 SPA, 백엔드 API, WebSocket 엔드포인트에 대한 리버스 프록시 설정을 담당한다.
#   - 한국 사용자 환경을 고려하여 타임아웃과 gzip 설정을 조정한다.

# 한국 트래픽 기준으로 적당한 keepalive 타임아웃을 설정한다.
keepalive_timeout  65;

# 정적 자산에 대해 gzip 압축을 활성화하여 초기 로딩 속도를 개선한다.
gzip on;
```

---

## 2. `design/` documents

Design docs MUST be written in **Korean**.
Filenames should be in **English** and reflect version and topic.

### 2.1 Directory structure (recommended)

```text
design/
  backend/
    initial-design.md
    v0.1.0-auth-and-core-game.md
    v0.2.0-profile-and-session.md
    v0.3.0-ranking-and-stats.md
    ...

  frontend/
    initial-design.md
    v0.1.0-lobby-and-game-ui.md
    v0.2.0-profile-and-navigation.md
    ...

  realtime/
    initial-design.md
    v0.1.0-game-loop-and-events.md
    v0.2.0-matchmaking-flow.md
    ...

  infra/
    initial-design.md
    v0.1.0-local-dev-stack.md
    v0.2.0-nginx-redis-setup.md
    v0.3.0-monitoring-stack.md
    ...
```

---

### 2.2 `initial-design.md` template (backend, Korean content)

```md
# 초기 전체 설계 (백엔드)

## 1. 목표
- 웹 기반 실시간 게임 서비스의 백엔드를 Spring Boot로 구현한다.
- 인증, 프로필, 게임, 매칭, 랭킹, 소셜/채팅, 토너먼트, 관리자 기능 등을 단계적으로 추가한다.

## 2. 주요 도메인
- Auth: 로그인/로그아웃, 토큰/세션 관리
- User/Profile: 계정 정보, 프로필, 기본 설정
- Game: 게임 룸, 게임 상태, 결과 기록
- Matchmaking: 매칭 큐, 매칭 알고리즘
- Rank: 레이팅/티어, 시즌 관리
- Social: 친구, 차단, 초대
- Chat: DM, 로비 채팅, 매치 채팅
- Tournament: 토너먼트 생성/참여/진행
- Admin: 유저 제재, 로그/기록 열람

## 3. 아키텍처 개요
- 계층 구조:
  - controller: HTTP/WebSocket 엔드포인트
  - service: 도메인 유스케이스 구현
  - domain: 엔티티/값 객체/도메인 서비스
  - repository: JPA 기반 영속성
- 데이터 흐름(예시):
  1. 클라이언트 요청 수신
  2. 인증/인가 체크
  3. 서비스 레이어에서 도메인 로직 처리
  4. DB/Redis 접근
  5. 응답 생성 및 반환

## 4. 버전 전략
- v0.x.y:
  - 기능을 점진적으로 추가하면서 구조를 안정화
- v1.0.0:
  - 포트폴리오 공개 가능한 수준의 안정된 기능/문서/테스트 제공
- 각 버전의 상세 목표는 VERSIONING.md를 따른다.

## 5. 품질/테스트
- 단위 테스트:
  - 핵심 서비스/도메인 로직에 대해 작성
- 통합 테스트:
  - 주요 유스케이스(회원가입, 로그인, 매치 시작/종료 등)를 검증
- 로그/모니터링:
  - 추후 버전에서 Prometheus/Grafana 등을 연계할 수 있도록 구조를 고려한다.
```

---

### 2.3 Per-version design doc template (backend, Korean content)

```md
# v0.1.0 - 인증 및 코어 게임 초기 버전 (백엔드)

## 1. 목적
- 최소한의 인증/사용자 관리와 한 판 게임을 저장할 수 있는 구조를 만든다.
- 프론트엔드/실시간 기능이 연동될 수 있는 API 골격을 제공한다.

## 2. 범위
- Auth
  - 회원가입, 로그인, 로그아웃
- User
  - 기본 사용자 엔티티/프로필 필드
- Game
  - 단일 게임 결과 저장을 위한 엔티티/테이블

## 3. API 설계 (요약)
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout
- GET  /api/users/me
- POST /api/games/result

각 API의 상세 요청/응답 스키마는 하위 섹션에 정의한다.

## 4. 도메인 모델
- User 엔티티:
  - id, username, passwordHash, nickname, createdAt, updatedAt
- GameResult 엔티티:
  - id, player1Id, player2Id, winnerId, score1, score2, playedAt

ERD 다이어그램 또는 간단한 표로 정리한다.

## 5. 처리 흐름
- 로그인 흐름:
  1. 사용자가 아이디/비밀번호를 전송
  2. UserRepository에서 사용자 조회
  3. 비밀번호 검증
  4. 세션 또는 토큰 발급
- 게임 결과 저장 흐름:
  1. 실시간 서비스에서 게임이 끝나면 결과를 백엔드 API로 전달
  2. GameResult 엔티티로 저장
  3. 이후 랭킹/통계에서 사용할 수 있도록 한다.

## 6. 테스트 전략
- 단위 테스트:
  - AuthService: 비밀번호 검증, 토큰 발급 로직
  - UserService: 프로필 조회/수정
- 통합 테스트:
  - /api/auth/register + /api/auth/login + /api/users/me 플로우
- 에러 케이스:
  - 중복 아이디, 잘못된 비밀번호, 미인증 상태 접근 등

## 7. 마이그레이션/호환성
- 이후 버전에서:
  - User 엔티티 확장(랭킹/소셜 정보 추가)
  - GameResult와 랭킹 시스템 연동
```

---

### 2.4 Frontend / realtime / infra docs

Use the same structure:

* **Title**: `# vX.Y.Z - <요약>` + 대상 영역 (프론트/실시간/인프라)
* **Sections**:

  1. 목적
  2. 범위
  3. 화면/플로우 또는 프로토콜 설계
  4. 내부 구조 (컴포넌트/스토어/핸들러 등)
  5. 테스트 전략
  6. 마이그레이션/호환성

All content MUST be in Korean, filenames English.

---

## 3. `CLONE_GUIDE.md` template (Korean-only)

Root-level `CLONE_GUIDE.md` MUST explain how to clone, configure, build, and run the whole system.

````md
# CLONE_GUIDE.md

## 1. 저장소 클론

```bash
git clone <REPO_URL>
cd <REPO_NAME>
```

## 2. 공통 요구사항

* OS:

  * Linux (Ubuntu 20.04 이상 권장) 또는 macOS
* 필수 도구:

  * Git
  * Docker, Docker Compose
  * (옵션) JDK 17, Node.js 18 이상 (로컬 직접 실행 시)

## 3. 환경 변수 설정

* 루트에 `.env` 파일을 두고 다음과 같은 값을 설정한다:

```bash
BACKEND_PORT=8080
FRONTEND_PORT=3000
DB_PORT=3306
REDIS_PORT=6379
TZ=Asia/Seoul
```

* 실제 값과 추가 변수들은 버전별로 이 문서를 업데이트하여 명시한다.

## 4. Docker 기반 통합 실행

```bash
docker compose up -d
```

* 주요 서비스:

  * backend: Spring Boot API/실시간 서버
  * frontend: React SPA
  * db: MariaDB
  * redis: Redis
  * nginx: 리버스 프록시

서비스 상태 확인:

```bash
docker compose ps
```

중지:

```bash
docker compose down
```

## 5. 개별 서비스 로컬 실행 (선택)

### 5.1 백엔드

```bash
cd backend
./gradlew bootRun   # 또는 mvn spring-boot:run
```

### 5.2 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

## 6. 테스트 실행

### 6.1 백엔드 테스트

```bash
cd backend
./gradlew test      # 또는 mvn test
```

### 6.2 프론트엔드 테스트

```bash
cd frontend
npm test
```

## 7. 버전별 주의사항

* 각 버전(v0.1.0, v0.2.0 등)에서 빌드/실행/테스트 절차가 달라지면,
  이 문서를 반드시 함께 수정해야 한다.
* 변경된 포트, 환경변수, 의존성은 버전별 섹션을 추가해서 명시한다.

```md
### v0.1.0

- 최소한의 백엔드/프론트엔드/DB/Redis만 사용한다.
- 모니터링/어드민 스택은 포함되지 않는다.

### v0.2.0

- 추가로 Nginx 리버스 프록시가 활성화된다.
- 새로 필요한 환경변수:
  - NGINX_HOST=...
```

````

---

## 4. Consistency checklist

When completing work for a version, agents MUST ensure:

### 4.1 Code

- All new/modified files:
  - Have header comments in Korean for significant modules.
  - Public functions/classes have Korean comments with:
    - Purpose
    - Inputs/outputs
    - Errors
    - Related tests
    - Related design docs
- Version tags (`vX.Y.Z`) in comments match `VERSIONING.md`.

### 4.2 Design docs (`design/`)

- For each version/domain:
  - At least one design doc exists:
    - `design/backend/vX.Y.Z-*.md`
    - `design/frontend/vX.Y.Z-*.md`
    - `design/realtime/vX.Y.Z-*.md`
    - `design/infra/vX.Y.Z-*.md`
  - Content is **Korean**.
  - It explains:
    - 목적 / 범위
    - 외부 동작(화면/API/프로토콜)
    - 내부 구조(모듈/컴포넌트/엔티티)
    - 테스트 전략
    - 마이그레이션/호환성

### 4.3 `CLONE_GUIDE.md`

- Reflects any changes to:
  - 환경변수
  - 의존성
  - 빌드/실행/테스트 절차
- All text is in Korean.

### 4.4 `VERSIONING.md`

- Target version:
  - Marked as completed/implemented when criteria are met.
  - Remaining 제한사항/추가 TODO가 있으면 명시.

---

## 5. Reuse in other repositories

Other repos can copy this file and:

- Change module/project names.
- Adjust design directory structure.
- Update tooling (build system, test frameworks).

The core rules MUST remain:

- This file is **English**, but it forces **Korean** comments/docs elsewhere.
- Code ↔ design docs ↔ clone guide stay consistent via:
  - Version tags
  - File path references
  - Explicit checklists.
```
