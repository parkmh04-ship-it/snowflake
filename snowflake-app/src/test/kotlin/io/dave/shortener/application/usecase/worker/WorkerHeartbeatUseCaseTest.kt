package io.dave.shortener.application.usecase.worker

import io.dave.shortener.domain.port.outbound.WorkerPort
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WorkerHeartbeatUseCase 테스트")
class WorkerHeartbeatUseCaseTest {

    private val workerPort: WorkerPort = mockk()
    private val useCase = WorkerHeartbeatUseCase(workerPort)

    @Test
    @DisplayName("워커 하트비트를 성공적으로 갱신한다")
    fun `heartbeat updates worker successfully`() = runTest {
        // given
        val workerNum = 1L
        coEvery { workerPort.updateUpdatedAt(listOf(workerNum), any()) } returns 1

        // when
        val result = useCase.heartbeat(workerNum)

        // then
        assertEquals(1, result)
    }

    @Test
    @DisplayName("워커가 존재하지 않아 하트비트 갱신에 실패하면 0을 반환한다")
    fun `heartbeat returns 0 if worker does not exist`() = runTest {
        // given
        val workerNum = 999L // 존재하지 않는 워커 번호
        coEvery { workerPort.updateUpdatedAt(listOf(workerNum), any()) } returns 0

        // when
        val result = useCase.heartbeat(workerNum)

        // then
        assertEquals(0, result)
    }
}
