package io.dave.shortener.application.usecase

import io.dave.shortener.domain.model.ShortUrl
import io.dave.shortener.domain.model.UrlMapping
import io.dave.shortener.domain.port.outbound.UrlPort
import org.springframework.stereotype.Service

/**
 * 단축 URL을 사용하여 원본 URL 매핑 정보를 조회하는 유스케이스 서비스입니다.
 * 이 서비스는 도메인 계층의 UrlPort를 사용하여 비즈니스 로직을 조율합니다.
 */
@Service
class RetrieveUrlUseCase(
    private val urlPort: UrlPort
) {

    /**
     * 주어진 단축 URL에 해당하는 원본 URL 매핑 정보를 조회합니다.
     *
     * @param shortUrlStr 조회할 단축 URL 문자열.
     * @return 해당하는 UrlMapping 객체, 없으면 null.
     */
    suspend fun retrieve(shortUrlStr: String): UrlMapping? {
        val shortUrl = ShortUrl(shortUrlStr)
        return urlPort.findByShortUrl(shortUrl)
    }
}