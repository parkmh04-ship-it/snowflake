package io.dave.snowflake.id.application.service

import io.dave.snowflake.domain.generator.IdGenerator
import io.dave.snowflake.id.application.port.inbound.GenerateGlobalTransactionIdUseCase
import io.dave.snowflake.id.domain.event.GlobalTransactionIdCreatedEvent
import io.dave.snowflake.id.domain.model.GlobalTransactionId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GlobalTransactionIdService(
    private val idGenerator: IdGenerator,
    private val eventPublisher: ApplicationEventPublisher
) : GenerateGlobalTransactionIdUseCase {

    override suspend fun generate(originGlobalTransactionId: Long?): GlobalTransactionId {
        // 1. ID 생성 (Snowflake)
        val newId = idGenerator.nextId()

        // 2. 도메인 객체 생성
        val domainModel = GlobalTransactionId(
            globalTransactionId = newId,
            originGlobalTransactionId = originGlobalTransactionId,
            createdAt = LocalDateTime.now()
        )

        // 3. 비동기 저장을 위한 이벤트 발행
        // Spring Event는 기본적으로 동기적이지만, 리스너에서 비동기 처리하거나
        // 비동기 이벤트 멀티캐스터를 설정할 수 있습니다.
        // 여기서는 리스너가 Coroutine Channel을 사용하여 버퍼링 및 비동기 처리를 수행한다고 가정합니다.
        eventPublisher.publishEvent(
            GlobalTransactionIdCreatedEvent(
                globalTransactionId = newId,
                originGlobalTransactionId = originGlobalTransactionId
            )
        )

        return domainModel
    }
}
