package io.dave.shortener.domain.port

/**
 * Snowflake 워커 ID 할당 및 관리에 필요한 데이터 접근 작업을 정의하는 Port 입니다.
 */
interface WorkerIdRepository {

    /**
     * 지정된 개수만큼 'IDLE' 상태의 워커 번호를 조회하고, 해당 워커들을 'ACTIVE' 상태로 업데이트합니다.
     * 트랜잭션 내에서 원자적으로 실행되어야 합니다.
     *
     * @param instanceId 워커를 할당받는 애플리케이션 인스턴스의 고유 식별자입니다.
     * @param requiredCount 필요한 워커 ID의 개수입니다.
     * @return 할당된 워커 번호(worker_num) 리스트를 반환합니다.
     * @throws RuntimeException 지정된 개수만큼의 워커 ID를 찾을 수 없는 경우 발생합니다.
     */
    suspend fun assignWorkerIds(instanceId: String, requiredCount: Int): List<Long>
}
