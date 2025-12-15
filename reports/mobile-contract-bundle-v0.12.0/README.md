# 모바일 계약 번들(v0.12.0) 복사 안내

## 포함 파일
- `contracts/openapi.json` (백엔드 OpenAPI 덤프)
- `contracts/ws-contract.md` (잡 진행률 WebSocket 계약)
- `contracts/SERVER_VERSION.txt` (서버 버전/커밋/추출 시각)

## 모바일 레포 복사 위치
- `mobile-repo/contracts/openapi.json`
- `mobile-repo/contracts/ws-contract.md`
- `mobile-repo/contracts/SERVER_VERSION.txt`

## 추출 정보
- OpenAPI 덤프 사용 URL: `http://localhost:8080/v3/api-docs` (yaml 엔드포인트는 401 응답으로 json 사용)
- WebSocket 경로/인증: `ws://<호스트>:8080/ws/jobs?token=<JWT>` / raw WebSocket, 쿼리 파라미터 `token` 필요
