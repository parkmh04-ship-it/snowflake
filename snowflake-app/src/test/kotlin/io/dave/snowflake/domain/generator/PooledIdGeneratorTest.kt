package io.dave.snowflake.domain.generator

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("PooledIdGenerator 단위 테스트")
class PooledIdGeneratorTest {

    private lateinit var mockSnowflakeIdGenerator1: SnowflakeIdGenerator
    private lateinit var mockSnowflakeIdGenerator2: SnowflakeIdGenerator
    private lateinit var mockSnowflakeIdGenerator3: SnowflakeIdGenerator
    private lateinit var meterRegistry: MeterRegistry

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 Mock 객체를 초기화합니다.
        mockSnowflakeIdGenerator1 = mockk()
        mockSnowflakeIdGenerator2 = mockk()
        mockSnowflakeIdGenerator3 = mockk()
        meterRegistry = SimpleMeterRegistry()
    }

    @Test
    @DisplayName("빈 SnowflakeIdGenerator 리스트로 초기화 시 IllegalArgumentException 발생")
    fun `should throw IllegalArgumentException when initialized with empty SnowflakeIdGenerator list`() {
        // given: 빈 SnowflakeIdGenerator 리스트
        val emptyGenerators = emptyList<SnowflakeIdGenerator>()

        // when & then: IllegalArgumentException이 발생하는지 검증
        val exception = assertThrows<IllegalArgumentException> {
            PooledIdGenerator(emptyGenerators, meterRegistry)
        }
        assertEquals("SnowflakeIdGenerator list cannot be empty.", exception.message)
    }

    @Test
    @DisplayName("유효한 SnowflakeIdGenerator 리스트로 초기화 시 PooledIdGenerator 인스턴스 생성")
    fun `should create PooledIdGenerator instance when initialized with valid SnowflakeIdGenerator list`() {
        // given: 유효한 SnowflakeIdGenerator 리스트
        val mockGenerators = listOf(mockSnowflakeIdGenerator1, mockSnowflakeIdGenerator2)

        // when: PooledIdGenerator 초기화
        val pooledIdGenerator = PooledIdGenerator(mockGenerators, meterRegistry)

        // then: PooledIdGenerator 인스턴스가 성공적으로 생성되었는지 확인
        assertNotNull(pooledIdGenerator)
    }

    @Test
    @DisplayName("nextId 호출 시 Round-Robin 방식으로 SnowflakeIdGenerator 호출 및 고유 ID 반환")
    fun `should call SnowflakeIdGenerator in Round-Robin fashion and return unique IDs`() = runBlocking {
        // given: 3개의 Mock SnowflakeIdGenerator 인스턴스
        val mockSnowflakeIdGenerators = listOf(
            mockSnowflakeIdGenerator1,
            mockSnowflakeIdGenerator2,
            mockSnowflakeIdGenerator3
        )

        // 각각의 nextId() 호출에 대해 Mock 객체가 다른 값을 반환하도록 설정
        every { runBlocking { mockSnowflakeIdGenerator1.nextId() } } returns 101L
        every { runBlocking { mockSnowflakeIdGenerator2.nextId() } } returns 202L
        every { runBlocking { mockSnowflakeIdGenerator3.nextId() } } returns 303L

        val pooledIdGenerator = PooledIdGenerator(mockSnowflakeIdGenerators, meterRegistry)

        // when: nextId를 여러 번 호출
        val id1 = pooledIdGenerator.nextId()
        val id2 = pooledIdGenerator.nextId()
        val id3 = pooledIdGenerator.nextId()
        val id4 = pooledIdGenerator.nextId() // 다시 첫 번째 generator 호출 예상

        // then: 각 SnowflakeIdGenerator가 Round-Robin 방식으로 호출되었는지, 고유한 ID를 반환했는지 검증
        assertEquals(101L, id1)
        assertEquals(202L, id2)
        assertEquals(303L, id3)
        assertEquals(101L, id4) // Round-Robin으로 다시 mockSnowflakeIdGenerator1 호출

        // 각 mockk의 nextId() 메서드가 정확히 호출되었는지 검증
        verify(exactly = 2) { runBlocking { mockSnowflakeIdGenerator1.nextId() } }
        verify(exactly = 1) { runBlocking { mockSnowflakeIdGenerator2.nextId() } }
        verify(exactly = 1) { runBlocking { mockSnowflakeIdGenerator3.nextId() } }

        confirmVerified(mockSnowflakeIdGenerator1, mockSnowflakeIdGenerator2, mockSnowflakeIdGenerator3)
    }
}