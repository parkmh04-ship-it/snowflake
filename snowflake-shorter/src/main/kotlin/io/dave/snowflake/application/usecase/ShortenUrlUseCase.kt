package io.dave.snowflake.application.usecase

import io.dave.snowflake.application.event.ShortUrlCreatedEvent
import io.dave.snowflake.domain.component.ShortUrlGenerator
import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.UrlMapping
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * 긴 URL을 단축 URL로 변환하고 저장하는 유스케이스 서비스입니다.
 * 이 서비스는 도메인 계층의 ShortUrlGenerator와 UrlPort를 사용하여 비즈니스 로직을 조율합니다.
 */
@Service
class ShortenUrlUseCase(
    private val shortUrlGenerator: ShortUrlGenerator,
    private val eventPublisher: ApplicationEventPublisher // 이벤트 발행기 추가
) {

    /**
     * 주어진 긴 URL을 단축합니다.
     * 이미 존재하는 긴 URL이라면 기존의 단축 URL 매핑을 반환하고,
     * 새로운 URL이라면 새로운 단축 URL을 생성하여 저장합니다.
     *
     * @param longUrlStr 단축할 원본 긴 URL 문자열.
     * @return 생성되거나 조회된 UrlMapping 객체.
     */
    suspend fun shorten(longUrlStr: String): UrlMapping {
        val longUrl = LongUrl(longUrlStr)

        // 1. 새로운 단축 URL 생성 (중복 검사 없이 바로 생성)
        val shortUrl = shortUrlGenerator.generate()
        val newMapping = UrlMapping(shortUrl, longUrl)

        // 2. 이벤트 발행 (DB 적재는 이벤트 리스너가 비동기로 처리)
        // 이벤트 발행에 실패하면 예외가 발생하여 캐시 저장 및 반환 로직이 실행되지 않습니다.
        eventPublisher.publishEvent(ShortUrlCreatedEvent(newMapping.shortUrl, newMapping.longUrl, newMapping.createdAt))

        // 4. 생성된 매핑 객체를 바로 반환 (응답 시간 최소화)
        return newMapping
    }
}