package io.dave.snowflake.adapter.outbound.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import io.dave.snowflake.adapter.outbound.persistence.entity.ShorterHistoryEntity
import io.dave.snowflake.adapter.outbound.persistence.repository.ShortUrlRepository
import io.dave.snowflake.config.IOX
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.model.UrlMapping
import io.dave.snowflake.domain.port.outbound.UrlPort
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class UrlPersistenceAdapter(
    private val repository: ShortUrlRepository,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,

    ) : UrlPort {

    override suspend fun save(mapping: UrlMapping): UrlMapping =
        withContext(Dispatchers.IOX) {
            val entity = ShorterHistoryEntity.fromDomain(mapping)
            val savedEntity = repository.save(entity)
            val domain = savedEntity.toDomain()

            // Cache Write-Through (Fire-and-Forget)
            cacheUrlMapping(domain)
            domain
        }

    override fun saveAll(mappings: Flow<UrlMapping>): Flow<UrlMapping> {
        return flow {
            val entities = mappings.toList().map { ShorterHistoryEntity.fromDomain(it) }
            if (entities.isNotEmpty()) {
                val savedEntities = withContext(Dispatchers.IOX) { repository.saveAll(entities) }
                savedEntities.forEach {
                    val domain = it.toDomain()
                    cacheUrlMapping(domain)
                    emit(domain)
                }
            }
        }
    }

    override suspend fun findByShortUrl(shortUrl: ShortUrl): UrlMapping? {
        val key = "short:${shortUrl.value}"
        return try {
            val cached = reactiveRedisTemplate.opsForValue()[key].awaitSingleOrNull()
            if (cached != null) {
                objectMapper.readValue(cached, UrlMapping::class.java)
            } else {
                findAndCache(shortUrl.value, key) { repository.findByShortUrl(it) }
            }
        } catch (e: Exception) {
            // Redis 오류 시 DB 조회로 Fallback
            findAndCache(shortUrl.value, key) { repository.findByShortUrl(it) }
        }
    }

    override suspend fun existsByShortUrl(shortUrl: ShortUrl): Boolean {
        val key = "short:${shortUrl.value}"
        val hasKey = reactiveRedisTemplate.hasKey(key).awaitSingleOrNull() ?: false
        return hasKey
    }

    private suspend fun findAndCache(
        identifier: String,
        key: String,
        dbQuery: (String) -> ShorterHistoryEntity?
    ): UrlMapping? =
        withContext(Dispatchers.IOX) {
            val entity = dbQuery(identifier)
            val domain = entity?.toDomain()
            if (domain != null) {
                cacheUrlMapping(domain)
            }
            domain
        }

    private fun cacheUrlMapping(domain: UrlMapping) {
        try {
            val json = objectMapper.writeValueAsString(domain)
            // ShortUrl Key
            reactiveRedisTemplate
                .opsForValue()
                .set("short:${domain.shortUrl.value}", json, Duration.ofMinutes(5))
                .subscribe()
        } catch (e: Exception) {
            log.error(e) { "cacheUrlMapping failed. ${e.message}" }
        }
    }

    companion object {
        private val log = KotlinLogging.logger { }
    }
}
