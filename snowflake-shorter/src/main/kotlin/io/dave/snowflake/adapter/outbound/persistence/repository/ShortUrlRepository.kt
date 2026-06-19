package io.dave.snowflake.adapter.outbound.persistence.repository

import io.dave.snowflake.adapter.outbound.persistence.entity.ShorterHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ShortUrlRepository : JpaRepository<ShorterHistoryEntity, Long> {
    fun findByShortUrl(shortUrl: String): ShorterHistoryEntity?
    fun findByLongUrl(longUrl: String): ShorterHistoryEntity?

    /** 주어진 단축 URL 집합 중 이미 존재하는 엔티티들을 조회합니다. 멱등 저장 시 중복 판별에 사용됩니다. */
    fun findAllByShortUrlIn(shortUrls: Collection<String>): List<ShorterHistoryEntity>

    /** 단축 URL의 DB 존재 여부를 확인합니다. 캐시 미스/만료 시 충돌 판별의 신뢰 가능한 소스입니다. */
    fun existsByShortUrl(shortUrl: String): Boolean
}
