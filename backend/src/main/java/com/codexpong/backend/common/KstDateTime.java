package com.codexpong.backend.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * [유틸리티] backend/src/main/java/com/codexpong/backend/common/KstDateTime.java
 * 설명:
 *   - 한국 표준시(Asia/Seoul) 기반의 시간 생성 및 직렬화를 위한 공통 헬퍼다.
 *   - API 응답에서 일관된 ISO-8601(+09:00) 포맷을 유지하도록 OffsetDateTime 변환을 제공한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 * 변경 이력:
 *   - v0.10.0: KST 고정 시계 및 응답 변환 도입
 */
public final class KstDateTime {

    public static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private KstDateTime() {
    }

    /**
     * 설명: 현재 시각을 한국 표준시 기준 LocalDateTime으로 반환한다.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(KST_ZONE);
    }

    /**
     * 설명: LocalDateTime 값을 한국 표준시 OffsetDateTime으로 변환한다.
     * 입력:
     *   - value: 변환 대상 LocalDateTime (null 허용)
     * 출력:
     *   - OffsetDateTime 또는 null
     */
    public static OffsetDateTime toOffset(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        ZonedDateTime zoned = value.atZone(KST_ZONE);
        return zoned.toOffsetDateTime();
    }
}
