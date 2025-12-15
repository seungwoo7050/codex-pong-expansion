package com.codexpong.backend.job;

import java.util.List;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/job/JobPageResponse.java
 * 설명:
 *   - v0.12.0 잡 리스트 API의 페이징 응답을 표현한다.
 *   - 상태/유형 필터가 적용된 결과를 클라이언트가 그대로 사용하도록 페이지 메타데이터를 포함한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
public record JobPageResponse(List<JobResponse> items, int page, int size, long totalItems, int totalPages) {
}
