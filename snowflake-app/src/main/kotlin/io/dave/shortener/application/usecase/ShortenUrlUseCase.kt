package io.dave.shortener.application.usecase

import io.dave.shortener.application.event.ShortUrlCreatedEvent
import io.dave.shortener.domain.component.ShortUrlGenerator
import io.dave.shortener.domain.model.LongUrl
import io.dave.shortener.domain.model.UrlMapping
import io.dave.shortener.domain.port.outbound.UrlPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * 긴 URL을 단축 URL로 변환하고 저장하는 유스케이스 서비스입니다.
 * 이 서비스는 도메인 계층의 ShortUrlGenerator와 UrlPort를 사용하여 비즈니스 로직을 조율합니다.
 */
@Service
class ShortenUrlUseCase(
    private val urlPort: UrlPort, // 기존 조회 기능은 UrlPort 유지
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
        
        // 1. 이미 존재하는 긴 URL인지 확인 (이것은 DB에 쿼리해야 하므로 UrlPort 유지)
        val existingMapping = urlPort.findByLongUrl(longUrl)
        if (existingMapping != null) {
            return existingMapping
        }

        // 2. 새로운 단축 URL 생성
        val shortUrl = shortUrlGenerator.generate()
        val newMapping = UrlMapping(shortUrl, longUrl)
        
        // 3. 이벤트 발행 (DB 적재는 이벤트 리스너가 비동기로 처리)
        eventPublisher.publishEvent(ShortUrlCreatedEvent(newMapping.shortUrl, newMapping.longUrl, newMapping.createdAt))
        
        // 4. 생성된 매핑 객체를 바로 반환 (응답 시간 최소화)
        return newMapping
    }
}