package io.dave.snowflake.id.application.service

import io.dave.snowflake.domain.generator.IdGenerator
import io.dave.snowflake.id.domain.event.GlobalTransactionIdCreatedEvent
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

@DisplayName("GlobalTransactionIdService 테스트")
class GlobalTransactionIdServiceTest {

    private val idGenerator: IdGenerator = mockk()
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private val service = GlobalTransactionIdService(idGenerator, eventPublisher)

    @Test
    @DisplayName("글로벌 트랜잭션 ID를 생성하고 이벤트를 발행해야 한다")
    fun `should generate id and publish event`() = runTest {
        // given
        val expectedId = 12345L
        coEvery { idGenerator.nextId() } returns expectedId

        // when
        val result = service.generate(null)

        // then
        assertEquals(expectedId, result.globalTransactionId)
        assertEquals(null, result.originGlobalTransactionId)
        assertNotNull(result.createdAt)

        verify(exactly = 1) { 
            eventPublisher.publishEvent(match<GlobalTransactionIdCreatedEvent> { 
                it.globalTransactionId == expectedId && it.originGlobalTransactionId == null 
            }) 
        }
    }

    @Test
    @DisplayName("원거래 ID가 있는 경우 이를 포함하여 생성하고 이벤트를 발행해야 한다")
    fun `should generate id with origin id and publish event`() = runTest {
        // given
        val expectedId = 67890L
        val originId = 12345L
        coEvery { idGenerator.nextId() } returns expectedId

        // when
        val result = service.generate(originId)

        // then
        assertEquals(expectedId, result.globalTransactionId)
        assertEquals(originId, result.originGlobalTransactionId)

        verify(exactly = 1) { 
            eventPublisher.publishEvent(match<GlobalTransactionIdCreatedEvent> { 
                it.globalTransactionId == expectedId && it.originGlobalTransactionId == originId 
            }) 
        }
    }
}
