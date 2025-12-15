package com.codexpong.backend.config;

import com.codexpong.backend.common.KstDateTime;
import jakarta.annotation.PostConstruct;
import java.util.Locale;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/config/LocaleConfig.java
 * 설명:
 *   - 서버 전체 기본 로케일과 타임존을 한국 표준시(KST)에 고정한다.
 *   - Jackson 직렬화 시 ISO-8601(+09:00) 문자열을 사용하도록 설정해 API 타임스탬프 포맷을 명확히 한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 * 변경 이력:
 *   - v0.10.0: KST/로케일 전역 설정 및 ISO 포맷 직렬화 추가
 */
@Configuration
public class LocaleConfig {

    @PostConstruct
    void initLocaleAndTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(KstDateTime.KST_ZONE));
        Locale.setDefault(Locale.KOREA);
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer kstObjectMapperCustomizer() {
        return builder -> builder
                .timeZone(TimeZone.getTimeZone(KstDateTime.KST_ZONE))
                .locale(Locale.KOREA)
                .simpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    }
}
