package io.dave.snowflake.adapter.outbound.persistence

import io.dave.snowflake.adapter.outbound.persistence.entity.ShortUrlEntity
import io.dave.snowflake.adapter.outbound.persistence.repository.ShortUrlRepository
import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.model.UrlMapping
import io.dave.snowflake.domain.port.outbound.UrlPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.ZoneId

@Repository
class UrlPersistenceAdapter(
    private val repository: ShortUrlRepository
) : UrlPort {

    override suspend fun save(mapping: UrlMapping): UrlMapping = withContext(Dispatchers.IO) {
        val entity = mapping.toEntity()
        val savedEntity = repository.save(entity)
        savedEntity.toDomain()
    }

    override fun saveAll(mappings: Flow<UrlMapping>): Flow<UrlMapping> {
        // Flow는 비동기 스트림이므로, 수집(collect)하여 리스트로 만든 뒤 배치 저장하고 다시 Flow로 변환
        // 주의: saveAll은 블로킹이므로 flow 빌더 내부에서 Dispatchers.IO로 감싸야 함
        return kotlinx.coroutines.flow.flow {
            val entities = mappings.toList().map { it.toEntity() }
            if (entities.isNotEmpty()) {
                val savedEntities = withContext(Dispatchers.IO) {
                    repository.saveAll(entities)
                }
                savedEntities.forEach { emit(it.toDomain()) }
            }
        }
    }

    @Cacheable(value = ["shortUrlCache"], key = "#shortUrl.value")
    override suspend fun findByShortUrl(shortUrl: ShortUrl): UrlMapping? = withContext(Dispatchers.IO) {
        repository.findByShortUrl(shortUrl.value)?.toDomain()
    }

    @Cacheable(value = ["longUrlCache"], key = "#longUrl.value")
    override suspend fun findByLongUrl(longUrl: LongUrl): UrlMapping? = withContext(Dispatchers.IO) {
        repository.findByLongUrl(longUrl.value)?.toDomain()
    }

    override suspend fun existsByShortUrl(shortUrl: ShortUrl): Boolean = withContext(Dispatchers.IO) {
        repository.findByShortUrl(shortUrl.value) != null
    }

    companion object {
        fun UrlMapping.toEntity(): ShortUrlEntity = ShortUrlEntity(
            shortUrl = this.shortUrl.value,
            longUrl = this.longUrl.value
        )

        fun ShortUrlEntity.toDomain(): UrlMapping = UrlMapping(
            shortUrl = ShortUrl(this.shortUrl),
            longUrl = LongUrl(this.longUrl),
            createdAt = this.createdAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L
        )
    }
}