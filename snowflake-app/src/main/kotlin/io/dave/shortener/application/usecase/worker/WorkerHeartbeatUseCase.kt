package io.dave.shortener.application.usecase.worker

import io.dave.shortener.domain.port.outbound.WorkerPort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Worker의 Heartbeat를 처리하여 Worker 상태를 주기적으로 갱신하는 유스케이스 서비스입니다.
 * Snowflake ID Generator의 Worker Management 기능을 지원합니다.
 */
@Service
class WorkerHeartbeatUseCase(
    private val workerPort: WorkerPort
) {

    /**
     * 현재 워커의 Heartbeat를 기록하여 `updatedAt` 필드를 갱신합니다.
     * 이를 통해 워커가 활성 상태임을 알리고, 유휴 워커 정제 대상에서 제외됩니다.
     *
     * @param workerNum Heartbeat를 보낼 워커의 고유 번호.
     * @return 갱신된 워커의 수 (성공 시 1, 실패 시 0).
     */
    suspend fun heartbeat(workerNum: Long): Int {
        // 특정 workerNum의 상태를 ACTIVE로 업데이트 (또는 IDLE 상태 워커만 업데이트) 및 updatedAt 갱신
        // 여기서는 간단히 updatedAt만 갱신하도록 구현
        return workerPort.updateUpdatedAt(listOf(workerNum), LocalDateTime.now())
    }
}
