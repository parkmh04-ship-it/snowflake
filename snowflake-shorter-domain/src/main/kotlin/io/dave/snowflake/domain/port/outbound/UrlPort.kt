package io.dave.snowflake.domain.port.outbound

import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.model.UrlMapping
import kotlinx.coroutines.flow.Flow

/** 출력 포트 (Output Port) - 저장소 인터페이스 도메인이 필요로 하는 저장소 기능을 정의합니다. 실제 구현은 어댑터 계층에서 이루어집니다. */
interface UrlPort {
    /** URL 매핑을 저장합니다. */
    suspend fun save(mapping: UrlMapping): UrlMapping

    /** 여러 URL 매핑을 일괄 저장합니다. */
    fun saveAll(mappings: Flow<UrlMapping>): Flow<UrlMapping>

    /** 단축 URL로 매핑을 조회합니다. */
    suspend fun findByShortUrl(shortUrl: ShortUrl): UrlMapping?

    /** 단축 URL이 이미 존재하는지 확인합니다. */
    suspend fun existsByShortUrl(shortUrl: ShortUrl): Boolean
}
