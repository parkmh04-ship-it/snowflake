package io.dave.snowflake.domain.generator

import java.util.concurrent.atomic.AtomicInteger

class PooledIdGenerator(
    private val idGenerators: List<IdGenerator>
) : IdGenerator {

    private val index = AtomicInteger(0)

    init {
        require(idGenerators.isNotEmpty()) { "IdGenerator list cannot be empty." }
    }

    override suspend fun nextId(): Long {
        // Round-Robin 방식으로 다음 생성기를 선택합니다.
        val currentIndex = index.getAndIncrement() % idGenerators.size
        // atomic increment는 음수가 될 수 있으므로 절대값 처리 혹은 로직 보완이 필요하지만,
        // 여기서는 리스트 사이즈로 모듈러 연산 전에 양수로 보정하는 것이 안전함.
        // 간단하게 Math.abs 사용보다 비트 연산으로 양수 변환 (0x7FFFFFFF)
        val safeIndex = (currentIndex and 0x7FFFFFFF) % idGenerators.size

        return idGenerators[safeIndex].nextId()
    }
}
