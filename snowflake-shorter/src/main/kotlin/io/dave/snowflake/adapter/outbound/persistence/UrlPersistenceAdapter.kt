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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class UrlPersistenceAdapter(
    private val repository: ShortUrlRepository,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : UrlPort {

    /**
     * URL 매핑을 멱등하게 저장합니다. Outbox Relay와 이벤트 리스너가 동일 매핑을 각각 저장할 수 있으므로(at-least-once),
     * 이미 같은 short_url이 존재하면 기존 행을 그대로 사용하여 UNIQUE 제약 위반 없이 성공으로 처리합니다.
     */
    override suspend fun save(mapping: UrlMapping): UrlMapping =
        withContext(Dispatchers.IOX) {
            val existing = repository.findByShortUrl(mapping.shortUrl.value)
            val domain =
                if (existing != null) {
                    existing.toDomain()
                } else {
                    try {
                        repository.save(ShorterHistoryEntity.fromDomain(mapping)).toDomain()
                    } catch (e: DataIntegrityViolationException) {
                        // 조회와 저장 사이에 동시 저장이 발생한 경쟁 상태 → 기존 행으로 폴백
                        repository.findByShortUrl(mapping.shortUrl.value)?.toDomain()
                            ?: throw e
                    }
                }

            // Cache Write-Through
            cacheUrlMapping(domain)
            domain
        }

    override fun saveAll(mappings: Flow<UrlMapping>): Flow<UrlMapping> {
        return flow {
            val incoming = mappings.toList()
            if (incoming.isEmpty()) return@flow

            val persisted =
                withContext(Dispatchers.IOX) {
                    val requested = incoming.map { it.shortUrl.value }
                    // 이미 존재하는 short_url은 건너뛰어 멱등성을 보장한다.
                    val existing =
                        repository.findAllByShortUrlIn(requested).associateBy { it.shortUrl }
                    val toInsert =
                        incoming
                            .filter { it.shortUrl.value !in existing }
                            .map { ShorterHistoryEntity.fromDomain(it) }

                    val inserted =
                        try {
                            if (toInsert.isNotEmpty()) repository.saveAll(toInsert) else emptyList()
                        } catch (e: DataIntegrityViolationException) {
                            // 조회 이후 동시 저장으로 인한 경쟁 상태 → 행 단위 멱등 저장으로 폴백
                            log.warn(e) { "Batch insert hit a unique conflict, retrying per-row idempotently." }
                            toInsert.mapNotNull { saveRowIdempotent(it) }
                        }

                    existing.values + inserted
                }

            persisted.forEach {
                val domain = it.toDomain()
                cacheUrlMapping(domain)
                emit(domain)
            }
        }
    }

    /** 단일 행을 멱등하게 저장한다. 이미 존재하면 기존 행을 반환한다. */
    private fun saveRowIdempotent(entity: ShorterHistoryEntity): ShorterHistoryEntity? =
        try {
            repository.save(entity)
        } catch (e: DataIntegrityViolationException) {
            repository.findByShortUrl(entity.shortUrl)
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
            log.error(e) { "Redis error occurred, falling back to DB for $shortUrl" }
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

    /** URL 매핑 정보를 Redis 캐시에 저장합니다. (Coroutines 스타일) */
    private suspend fun cacheUrlMapping(domain: UrlMapping) {
        try {
            val json = objectMapper.writeValueAsString(domain)
            val key = "short:${domain.shortUrl.value}"

            // .subscribe() 대신 awaitSingleOrNull() 사용하여 비동기 흐름 제어
            reactiveRedisTemplate
                .opsForValue()
                .set(key, json, Duration.ofMinutes(5))
                .awaitSingleOrNull()
        } catch (e: Exception) {
            log.error(e) { "cacheUrlMapping failed for ${domain.shortUrl.value}" }
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
