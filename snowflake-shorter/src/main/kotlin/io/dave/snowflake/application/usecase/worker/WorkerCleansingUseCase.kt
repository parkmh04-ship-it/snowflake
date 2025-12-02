package io.dave.snowflake.application.usecase.worker

import io.dave.snowflake.domain.model.WorkerStatus
import io.dave.snowflake.domain.port.outbound.WorkerPort
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 비활성 상태의 Worker ID를 주기적으로 회수하고 관리하는 유스케이스 서비스입니다.
 * Snowflake ID Generator의 Worker Management 기능을 지원합니다.
 */
@Service
class WorkerCleansingUseCase(
    private val workerPort: WorkerPort
) {

    /**
     * 일정 시간 이상 업데이트되지 않은 ACTIVE 상태의 워커를 찾아 회수합니다.
     * 회수된 워커는 다시 사용 가능한 상태가 됩니다.
     *
     * @return 회수된 워커의 수.
     */
    suspend fun cleanseIdleWorkers(): Int {
        val threshold = LocalDateTime.now().minusMinutes(5) // 5분 이상 업데이트되지 않은 워커
        val idleWorkers = workerPort.findByStatusAndUpdatedAtBefore(WorkerStatus.ACTIVE, threshold).toList()
        
        if (idleWorkers.isEmpty()) {
            return 0
        }

        val workerNumsToCleanse = idleWorkers.map { it.workerNum }
        return workerPort.cleanseWorkers(workerNumsToCleanse, LocalDateTime.now())
    }
}
