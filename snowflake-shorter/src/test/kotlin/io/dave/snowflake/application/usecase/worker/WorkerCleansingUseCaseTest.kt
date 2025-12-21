package io.dave.snowflake.application.usecase.worker

import io.dave.snowflake.domain.model.WorkerStatus
import io.dave.snowflake.domain.port.outbound.WorkerPort
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("WorkerCleansingUseCase 테스트")
class WorkerCleansingUseCaseTest {

    private val workerPort: WorkerPort = mockk()
    private val useCase = WorkerCleansingUseCase(workerPort)

    @Test
    @DisplayName("유휴 워커를 성공적으로 정화한다")
    fun `cleanse idle workers successfully`() = runTest {
        // given
        val threshold = LocalDateTime.now().minusMinutes(5)
        val worker1 = io.dave.snowflake.domain.model.Worker(
            id = 1L,
            workerNum = 1L,
            workerName = "worker-1",
            status = WorkerStatus.ACTIVE,
            updatedAt = threshold.minusMinutes(1)
        )
        val worker2 = io.dave.snowflake.domain.model.Worker(
            id = 2L,
            workerNum = 2L,
            workerName = "worker-2",
            status = WorkerStatus.ACTIVE,
            updatedAt = threshold.minusMinutes(2)
        )

        coEvery { workerPort.findByStatusAndUpdatedAtBefore(WorkerStatus.ACTIVE, any()) } returns flowOf(
            worker1,
            worker2
        )
        coEvery { workerPort.cleanseWorkers(listOf(1L, 2L), any()) } returns 2

        // when
        val result = useCase.cleanseIdleWorkers()

        // then
        assertEquals(2, result)
    }

    @Test
    @DisplayName("정화할 유휴 워커가 없으면 0을 반환한다")
    fun `return 0 if no idle workers to cleanse`() = runTest {
        // given
        coEvery { workerPort.findByStatusAndUpdatedAtBefore(WorkerStatus.ACTIVE, any()) } returns emptyFlow()

        // when
        val result = useCase.cleanseIdleWorkers()

        // then
        assertEquals(0, result)
    }
}
