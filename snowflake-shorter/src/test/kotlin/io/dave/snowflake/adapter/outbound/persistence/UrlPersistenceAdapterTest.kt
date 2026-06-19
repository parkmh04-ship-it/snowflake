package io.dave.snowflake.adapter.outbound.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import io.dave.snowflake.adapter.outbound.persistence.entity.ShorterHistoryEntity
import io.dave.snowflake.adapter.outbound.persistence.repository.ShortUrlRepository
import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.model.UrlMapping
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono

@DisplayName("UrlPersistenceAdapter 멱등 저장 테스트")
class UrlPersistenceAdapterTest {

    private val repository: ShortUrlRepository = mockk()
    private val redisTemplate: ReactiveRedisTemplate<String, String> = mockk()
    private val valueOps: ReactiveValueOperations<String, String> = mockk()
    private val objectMapper = ObjectMapper()

    private val adapter = UrlPersistenceAdapter(repository, redisTemplate, objectMapper)

    private fun stubCache() {
        every { redisTemplate.opsForValue() } returns valueOps
        every { valueOps.set(any<String>(), any<String>(), any<java.time.Duration>()) } returns
            Mono.just(true)
    }

    private fun mapping(short: String) = UrlMapping(ShortUrl(short), LongUrl("https://example.com/$short"))

    private fun entity(short: String) =
        ShorterHistoryEntity(id = 1L, shortUrl = short, longUrl = "https://example.com/$short")

    @Test
    @DisplayName("saveAll: 이미 존재하는 short_url은 재삽입하지 않고 신규만 저장한다")
    fun `saveAll skips existing and inserts only new`() = runTest {
        stubCache()
        // "dup"은 이미 존재, "new"는 신규
        every { repository.findAllByShortUrlIn(any()) } returns listOf(entity("dup"))
        val insertSlot = slot<List<ShorterHistoryEntity>>()
        every { repository.saveAll<ShorterHistoryEntity>(capture(insertSlot)) } answers
            { insertSlot.captured.toMutableList() }

        val result =
            adapter.saveAll(flowOf(mapping("dup"), mapping("new"))).toList()

        // 신규 "new"만 insert 대상
        assertEquals(listOf("new"), insertSlot.captured.map { it.shortUrl })
        // 결과는 기존+신규 모두 방출
        assertEquals(setOf("dup", "new"), result.map { it.shortUrl.value }.toSet())
    }

    @Test
    @DisplayName("saveAll: 전부 이미 존재하면 saveAll을 호출하지 않는다")
    fun `saveAll inserts nothing when all exist`() = runTest {
        stubCache()
        every { repository.findAllByShortUrlIn(any()) } returns listOf(entity("a"), entity("b"))

        val result = adapter.saveAll(flowOf(mapping("a"), mapping("b"))).toList()

        assertEquals(setOf("a", "b"), result.map { it.shortUrl.value }.toSet())
        verify(exactly = 0) { repository.saveAll<ShorterHistoryEntity>(any()) }
    }

    @Test
    @DisplayName("save: 이미 존재하면 재삽입 없이 기존 행을 반환한다")
    fun `save is idempotent when row already exists`() = runTest {
        stubCache()
        every { repository.findByShortUrl("exist") } returns entity("exist")

        val result = adapter.save(mapping("exist"))

        assertEquals("exist", result.shortUrl.value)
        verify(exactly = 0) { repository.save<ShorterHistoryEntity>(any()) }
    }
}
