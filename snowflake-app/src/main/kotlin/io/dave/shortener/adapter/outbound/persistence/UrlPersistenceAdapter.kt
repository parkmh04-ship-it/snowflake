package io.dave.shortener.adapter.outbound.persistence

import io.dave.shortener.adapter.outbound.persistence.entity.ShortUrlEntity
import io.dave.shortener.adapter.outbound.persistence.repository.ShortUrlR2dbcRepository
import io.dave.shortener.domain.model.LongUrl
import io.dave.shortener.domain.model.ShortUrl
import io.dave.shortener.domain.model.UrlMapping
import io.dave.shortener.domain.port.outbound.UrlPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.ZoneId

@Repository
class UrlPersistenceAdapter(
    private val r2dbcRepository: ShortUrlR2dbcRepository
) : UrlPort {

    override suspend fun save(mapping: UrlMapping): UrlMapping {
        val entity = mapping.toEntity()
        val savedEntity = r2dbcRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun saveAll(mappings: Flow<UrlMapping>): Flow<UrlMapping> {
        val entities = mappings.map { it.toEntity() }
        return r2dbcRepository.saveAll(entities)
            .map { it.toDomain() }
    }

    @Cacheable(value = ["shortUrlCache"], key = "#shortUrl.value")
    override suspend fun findByShortUrl(shortUrl: ShortUrl): UrlMapping? {
        return r2dbcRepository.findByShortUrl(shortUrl.value)?.toDomain()
    }

    @Cacheable(value = ["longUrlCache"], key = "#longUrl.value")
    override suspend fun findByLongUrl(longUrl: LongUrl): UrlMapping? {
        return r2dbcRepository.findByLongUrl(longUrl.value)?.toDomain()
    }

    override suspend fun existsByShortUrl(shortUrl: ShortUrl): Boolean {
        return findByShortUrl(shortUrl) != null
    }

    companion object {
        /**
         * 도메인 모델(UrlMapping)과 영속성 엔티티(ShortUrlEntity) 간의 변환을 담당하는 확장 함수들을 제공합니다.
         * Hexagonal Architecture의 Adapter 계층에서 도메인과 인프라 간의 매핑을 처리합니다.
         */

        /**
         * UrlMapping 도메인 모델을 ShortUrlEntity 영속성 엔티티로 변환합니다.
         * @return ShortUrlEntity 변환된 영속성 엔티티
         */
        fun UrlMapping.toEntity(): ShortUrlEntity = ShortUrlEntity(
            shortUrl = this.shortUrl.value,
            longUrl = this.longUrl.value
        )

        /**
         * ShortUrlEntity 영속성 엔티티를 UrlMapping 도메인 모델로 변환합니다.
         * @return UrlMapping 변환된 도메인 모델
         */
        fun ShortUrlEntity.toDomain(): UrlMapping = UrlMapping(
            shortUrl = ShortUrl(this.shortUrl),
            longUrl = LongUrl(this.longUrl),
            createdAt = this.createdAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L
        )

    }
}