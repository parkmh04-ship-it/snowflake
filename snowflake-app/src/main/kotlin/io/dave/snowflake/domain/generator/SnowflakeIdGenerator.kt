package io.dave.snowflake.domain.generator

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * 분산 환경에서 유일한 ID를 생성하기 위한 Snowflake ID 생성기 구현체.
 *
 * Snowflake ID는 64비트 정수이며, 다음과 같은 구성으로 이루어져 있습니다.
 * 1. 타임스탬프 (41비트): 에폭 시간(여기서는 `System.currentTimeMillis()` 기준)으로부터의 경과 시간을 밀리초 단위로 나타냅니다.
 *    이를 통해 약 69년간 유일한 ID를 생성할 수 있습니다.
 * 2. 워커 ID (10비트): ID를 생성하는 머신 또는 프로세스(워커)의 고유 ID입니다.
 *    최대 1024 (2^10)개의 워커를 지원합니다.
 * 3. 시퀀스 (12비트): 동일 밀리초 내에서 여러 ID를 생성할 때 사용되는 시퀀스 번호입니다.
 *    밀리초당 최대 4096 (2^12)개의 ID를 생성할 수 있습니다.
 *
 * 이 구현체는 `kotlinx.coroutines.sync.Mutex`를 사용하여 `nextId` 메서드의 동시성 문제를 해결하고,
 * `timeSource`를 주입받아 테스트 용이성을 높였습니다.
 *
 * @property workerId 이 ID 생성기가 속한 워커의 고유 ID (0-1023 범위).
 * @property timeSource 현재 시간을 밀리초 단위로 반환하는 함수. 기본값은 `System::currentTimeMillis`.
 */
class SnowflakeIdGenerator(
    private val workerId: Long,
    meterRegistry: MeterRegistry? = null,
    private val timeSource: () -> Long = System::currentTimeMillis
) {

    /** 마지막으로 ID를 생성한 타임스탬프 (밀리초 단위) */
    private var lastTimestamp = -1L
    /** 동일 밀리초 내에서 생성된 ID의 시퀀스 번호 */
    private var sequence = 0L
    /** `nextId` 메서드의 동시성 제어를 위한 뮤텍스 */
    private val mutex = Mutex()

    /** 워커 ID에 할당된 비트 수 */
    private val workerIdBits = 10L
    /** 시퀀스에 할당된 비트 수 */
    private val sequenceBits = 12L

    /** 최대 워커 ID (2^10 - 1) */
    private val maxWorkerId = (-1L shl workerIdBits.toInt()).inv()
    /** 최대 시퀀스 번호 (2^12 - 1) */
    private val maxSequence = (-1L shl sequenceBits.toInt()).inv()

    /** 워커 ID를 타임스탬프 비트만큼 왼쪽으로 이동시키기 위한 시프트 값 */
    private val workerIdShift = sequenceBits
    /** 타임스탬프를 가장 상위 비트로 이동시키기 위한 시프트 값 */
    private val timestampLeftShift = sequenceBits + workerIdBits

    private val exhaustionCounter = meterRegistry?.let {
        Counter.builder("snowflake.sequence.exhaustion.total")
            .description("Total number of times the sequence was exhausted in a millisecond")
            .tag("workerId", workerId.toString())
            .register(it)
    }

    init {
        require(workerId in 0..maxWorkerId) { "Worker ID must be between 0 and $maxWorkerId" }
    }

    /**
     * 다음 Snowflake ID를 생성하여 반환합니다.
     * 이 메서드는 스레드 안전하며 (코루틴 안전), 동일 밀리초 내에서 호출될 경우 시퀀스를 증가시키고,
     * 밀리초가 변경되면 시퀀스를 0으로 초기화합니다.
     *
     * 클럭이 뒤로 이동한 경우 `RuntimeException`을 발생시켜 데이터 일관성을 유지합니다.
     *
     * @return 생성된 유일한 64비트 Snowflake ID.
     * @throws RuntimeException 클럭이 뒤로 이동했을 경우 발생.
     */
    suspend fun nextId(): Long = mutex.withLock {
        var currentTimestamp = timeGen()

        if (currentTimestamp < lastTimestamp) {
            throw RuntimeException("Clock moved backwards. Refusing to generate id for ${lastTimestamp - currentTimestamp} milliseconds")
        }

        if (currentTimestamp == lastTimestamp) {
            // 동일 밀리초 내에서 시퀀스 증가. 최대 시퀀스에 도달하면 다음 밀리초로 넘어감.
            sequence = (sequence + 1) and maxSequence
            if (sequence == 0L) {
                exhaustionCounter?.increment()
                currentTimestamp = tilNextMillis(lastTimestamp)
            }
        } else {
            // 밀리초가 변경되었으므로 시퀀스 초기화
            sequence = 0L
        }

        lastTimestamp = currentTimestamp

        // 각 구성 요소를 비트 시프트하여 최종 ID 조합
        (currentTimestamp shl timestampLeftShift.toInt()) or
                (workerId shl workerIdShift.toInt()) or
                sequence
    }

    /**
     * 마지막 타임스탬프보다 큰 다음 밀리초를 기다립니다.
     * 주로 동일 밀리초 내 시퀀스가 고갈되었을 때 호출됩니다.
     *
     * @param lastTimestamp 마지막으로 ID를 생성한 타임스탬프.
     * @return lastTimestamp보다 큰 새로운 타임스탬프.
     */
    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp = timeGen()
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen()
        }
        return timestamp
    }

    /**
     * 현재 시간을 밀리초 단위로 반환합니다. `timeSource` 함수를 사용합니다.
     *
     * @return 현재 시간을 밀리초 단위로 나타내는 Long 값.
     */
    private fun timeGen(): Long = timeSource()
}