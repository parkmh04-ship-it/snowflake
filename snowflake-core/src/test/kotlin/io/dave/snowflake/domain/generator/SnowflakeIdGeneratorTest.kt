package io.dave.snowflake.domain.generator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("SnowflakeIdGenerator 테스트")
class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("Worker ID가 범위를 벗어나면 예외가 발생한다")
    fun `init should throw exception when workerId is out of range`() {
        val maxWorkerId = (-1L shl 10).inv() // 1023
        assertThrows<IllegalArgumentException> {
            SnowflakeIdGenerator(maxWorkerId + 1)
        }
        assertThrows<IllegalArgumentException> {
            SnowflakeIdGenerator(-1)
        }
    }

    @DisplayName("nextId는 올바른 워커 ID를 포함하는 유니크한 ID를 생성한다")
    fun `nextId should generate unique id with correct worker id`() = runTest {
        // 1. 예측 가능한 타임스탬프를 고정합니다. (예: 2025년 1월 1일 00:00:00.000 UTC)
        // 실제 Snowflake 알고리즘은 Epoch Time(예: 2020-01-01)부터의 경과 시간을 사용하지만,
        // 여기서는 System.currentTimeMillis()를 기준으로 합니다.
        val fixedTimestamp = 1735689600000L // 2025-01-01 (임의의 고정값)

        val workerId = 123L
        // 2. timeSource를 람다로 주입하여 항상 고정된 값을 반환하도록 합니다.
        val generator = SnowflakeIdGenerator(
            workerId = workerId,
            timeSource = { fixedTimestamp }
        )
        val id = generator.nextId()

        val extractedWorkerId = (id shr 12) and 0x3FF

        // 3. 고정된 타임스탬프와 워커 ID로 예측값을 계산합니다.
        val workerIdShift = 12L
        val timestampLeftShift = 12L + 10L // 22L
        val expectedId = (fixedTimestamp shl timestampLeftShift.toInt()) or (workerId shl workerIdShift.toInt())

        // 검증
        Assertions.assertEquals(workerId, extractedWorkerId)
        Assertions.assertEquals(expectedId, id) // 계산된 예측값 사용
    }

    @Test
    @DisplayName("같은 밀리초 내에서 호출되면 시퀀스가 증가한다")
    fun `nextId should increment sequence within same millisecond`() = runTest {
        val fixedTime = 1000L
        val generator = SnowflakeIdGenerator(1L) { fixedTime }

        val id1 = generator.nextId()
        val id2 = generator.nextId()

        // 시퀀스는 하위 12비트에 위치함 (0xFFF)
        val sequence1 = id1 and 0xFFF
        val sequence2 = id2 and 0xFFF

        // 타임스탬프가 고정되어 있으므로 시퀀스가 1 증가해야 함
        Assertions.assertEquals(sequence1 + 1, sequence2)
        // 타임스탬프 부분도 검증
        Assertions.assertEquals(fixedTime, id1 shr 22)
        Assertions.assertEquals(fixedTime, id2 shr 22)
    }

    @Test
    @DisplayName("병렬 코루틴에서 호출해도 중복되지 않는다")
    fun `nextId should be thread-safe (coroutine-safe)`() = runBlocking(Dispatchers.Default) {
        val generator = SnowflakeIdGenerator(1L)
        val count = 10000
        val deferredIds = (1..count).map {
            async { generator.nextId() }
        }
        val ids = deferredIds.awaitAll().toSet()
        assertEquals(count, ids.size)
    }
}