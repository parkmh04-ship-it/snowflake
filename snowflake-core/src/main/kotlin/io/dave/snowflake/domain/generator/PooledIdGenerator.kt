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
        // getAndIncrement()가 Int.MAX_VALUE를 넘어 음수로 오버플로되더라도 항상 양수 인덱스를
        // 얻도록, 부호 비트를 제거(and Int.MAX_VALUE)한 뒤 모듈러 연산을 적용합니다.
        val safeIndex = (index.getAndIncrement() and Int.MAX_VALUE) % idGenerators.size

        return idGenerators[safeIndex].nextId()
    }
}
