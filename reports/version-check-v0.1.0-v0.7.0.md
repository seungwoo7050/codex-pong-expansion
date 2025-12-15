# v0.1.0~v0.7.0 구현 상태 점검 (소스 코드 기준)

## 목적
- `VERSIONING.md`에 정의된 v0.1.0~v0.7.0 범위의 구현을 **소스 코드 기준**으로 재확인한다.
- 기존 "전부 미구현" 결론이 잘못되었음을 바로잡고, 실제로 존재하는 코드와 기능 범위를 설명한다.

## 주요 소스 코드 현황 요약
- **백엔드 전체 스택 존재**: Spring Boot 애플리케이션 엔트리포인트와 헬스 체크, 인증, 사용자 프로필, 게임/랭킹, 소셜, 채팅, 토너먼트 도메인까지 컨트롤러·서비스·엔티티·리포지토리가 구현돼 있다. 예) 헬스 체크 `HealthController`, 게임 결과 `GameResultController`, 인증 `AuthController` 등.【F:backend/src/main/java/com/codexpong/backend/health/HealthController.java†L1-L39】【F:backend/src/main/java/com/codexpong/backend/game/GameResultController.java†L1-L35】【F:backend/src/main/java/com/codexpong/backend/auth/controller/AuthController.java†L1-L56】
- **실시간 처리 포함**: WebSocket 에코 핸들러, 게임 방 실시간 입력 처리, 채팅 WebSocket 핸들러 등이 존재해 실시간 흐름이 구현돼 있다.【F:backend/src/main/java/com/codexpong/backend/game/EchoWebSocketHandler.java†L1-L34】【F:backend/src/main/java/com/codexpong/backend/game/GameWebSocketHandler.java†L1-L83】
- **게임 엔진 로직**: 틱 기반 물리 시뮬레이션을 수행하는 `GameEngine`이 포함되어 있고, 패들/공 이동 및 득점 판정 로직이 구현돼 있다.【F:backend/src/main/java/com/codexpong/backend/game/engine/GameEngine.java†L1-L64】
- **프론트엔드 존재**: React + TypeScript 기반 SPA가 있으며, 로그인/회원가입/로비/게임/리더보드/토너먼트/친구 관리 페이지 라우팅이 구성돼 있다.【F:frontend/src/App.tsx†L1-L70】
- **테스트 코드 존재**: 백엔드 단위/통합 테스트가 다수 작성돼 있으며, 게임 엔진/매칭/랭킹/소셜/채팅/인증 흐름을 검증하는 테스트 클래스들이 포함돼 있다.【F:backend/src/test/java/com/codexpong/backend/game/engine/GameEngineTest.java†L1-L26】【F:backend/src/test/java/com/codexpong/backend/game/service/MatchmakingServiceTest.java†L1-L25】【F:backend/src/test/java/com/codexpong/backend/auth/AuthIntegrationTest.java†L1-L24】【F:backend/src/test/java/com/codexpong/backend/social/SocialFlowTest.java†L1-L25】【F:backend/src/test/java/com/codexpong/backend/chat/ChatFlowTest.java†L1-L27】

## 버전별 코드 관점 판단
### v0.1.0 (Core skeleton & minimal vertical slice)
- **헬스 체크 및 최소 API**: `/api/health` 엔드포인트가 구현되어 서비스 상태를 반환하며, 게임 결과 조회 API와 기본 WebSocket 에코 핸들러가 존재한다.【F:backend/src/main/java/com/codexpong/backend/health/HealthController.java†L1-L39】【F:backend/src/main/java/com/codexpong/backend/game/GameResultController.java†L1-L35】【F:backend/src/main/java/com/codexpong/backend/game/EchoWebSocketHandler.java†L1-L34】
- **프론트엔드 최소 흐름**: 라우팅이 설정되어 로비/게임 화면에 접근 가능하며, 보호 라우트로 인증 여부를 검사하는 구조가 준비돼 있다.【F:frontend/src/App.tsx†L1-L70】
- **결론**: 코드 기준으로 v0.1.0 요구 범위를 충족하는 스캐폴드와 최소 상호작용 흐름이 구현돼 있다.

### v0.2.0 (Accounts, authentication & basic profile)
- **인증/회원가입**: `AuthController`와 `AuthService`가 JWT 기반 회원가입·로그인·로그아웃을 제공하고, 보안 설정이 `SecurityConfig`에 정의돼 있다.【F:backend/src/main/java/com/codexpong/backend/auth/controller/AuthController.java†L1-L56】【F:backend/src/main/java/com/codexpong/backend/config/SecurityConfig.java†L1-L71】
- **프로필 관리**: `UserController`와 `UserService`가 프로필 조회/수정 API를 제공한다.【F:backend/src/main/java/com/codexpong/backend/user/controller/UserController.java†L1-L47】
- **결론**: 계정·인증·기본 프로필 흐름이 코드로 구현돼 있어 v0.2.0 범위가 충족된다.

### v0.3.0 (Real-time 1v1 game & simple matchmaking)
- **게임 엔진과 룸**: `GameEngine`이 틱 기반 물리 시뮬레이션을 수행하며, `GameRoomService`가 방 상태와 입력 처리를 관리한다.【F:backend/src/main/java/com/codexpong/backend/game/engine/GameEngine.java†L1-L64】【F:backend/src/main/java/com/codexpong/backend/game/service/GameRoomService.java†L1-L74】
- **매칭 및 WebSocket**: `MatchmakingController`가 매칭 요청을 받고, `GameWebSocketHandler`가 실시간 입력을 처리한다.【F:backend/src/main/java/com/codexpong/backend/game/MatchmakingController.java†L1-L46】【F:backend/src/main/java/com/codexpong/backend/game/GameWebSocketHandler.java†L1-L83】
- **결론**: 실시간 경기/매칭 흐름이 구현돼 있어 v0.3.0 기능이 코드에 반영되어 있다.

### v0.4.0 (Ranked mode & basic ranking system)
- **랭크 매칭/리더보드**: `RankingService`와 `RankingController`, `RankedMatchmakingController`가 랭크 점수 계산과 리더보드/랭크 매칭 API를 제공한다.【F:backend/src/main/java/com/codexpong/backend/game/service/RankingService.java†L1-L60】【F:backend/src/main/java/com/codexpong/backend/game/RankingController.java†L1-L49】【F:backend/src/main/java/com/codexpong/backend/game/RankedMatchmakingController.java†L1-L52】
- **결론**: 랭크 계산·리더보드·랭크 매칭 코드가 존재해 v0.4.0 요구가 구현돼 있다.

### v0.5.0 (Friends & invitations)
- **친구/차단/초대**: `SocialController`와 관련 서비스/리포지토리가 친구 요청, 초대, 차단을 처리하고 WebSocket 알림을 발행한다.【F:backend/src/main/java/com/codexpong/backend/social/controller/SocialController.java†L1-L62】【F:backend/src/main/java/com/codexpong/backend/social/service/SocialService.java†L1-L67】
- **결론**: 친구 관리와 초대/차단 흐름이 코드로 구현돼 있어 v0.5.0 범위가 반영돼 있다.

### v0.6.0 (Chat: DM, lobby, match chat)
- **채팅 도메인**: `ChatController`, `ChatService`, `ChatWebSocketHandler`가 DM/채널 메시지 저장 및 실시간 송수신을 처리하며, 뮤트/히스토리 조회 로직이 포함돼 있다.【F:backend/src/main/java/com/codexpong/backend/chat/controller/ChatController.java†L1-L59】【F:backend/src/main/java/com/codexpong/backend/chat/service/ChatService.java†L1-L72】【F:backend/src/main/java/com/codexpong/backend/chat/ChatWebSocketHandler.java†L1-L75】
- **결론**: 채팅 API와 실시간 흐름이 코드에 구현되어 v0.6.0 요구를 충족한다.

### v0.7.0 (Tournaments & events)
- **토너먼트 도메인**: `TournamentController`와 `TournamentService`가 토너먼트 생성/참여/진행 로직을 제공하고, `TournamentWebSocketHandler`가 실시간 상태 전파를 담당한다.【F:backend/src/main/java/com/codexpong/backend/tournament/controller/TournamentController.java†L1-L72】【F:backend/src/main/java/com/codexpong/backend/tournament/service/TournamentService.java†L1-L63】【F:backend/src/main/java/com/codexpong/backend/tournament/TournamentWebSocketHandler.java†L1-L62】
- **결론**: 토너먼트 생성·진행·실시간 전파 기능이 구현돼 있어 v0.7.0 범위를 충족한다.

## 테스트 및 실행 상태
- 백엔드에 다수의 테스트가 존재하지만, 로컬 실행 시 `gradle-wrapper.jar` 부재로 `./gradlew test`를 실행하지 못했다. 추가 실행 전 wrapper 다운로드가 필요하다.【68d8c2†L1-L3】

## 종합 결론
- 기존 "백엔드/실시간/테스트 코드 부재" 결론은 사실이 아니다. v0.1.0~v0.7.0 각 버전에 해당하는 기능(헬스 체크, 인증/프로필, 실시간 게임 및 매칭, 랭크, 친구/초대, 채팅, 토너먼트)이 모두 소스 코드와 테스트로 구현되어 있다. 현재 레포의 소스코드는 요구 버전 기능을 포괄하는 완성된 애플리케이션 코드이며, 추가 검증을 위해서는 gradle wrapper를 준비한 뒤 테스트 실행이 필요하다.
