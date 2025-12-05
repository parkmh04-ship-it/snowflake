package io.dave.snowflake.domain.model

/**
 * 실패한 이벤트를 나타내는 도메인 모델입니다.
 * Dead Letter Queue에 저장되어 나중에 재처리됩니다.
 *
 * @property id 고유 식별자
 * @property shortUrl 단축 URL
 * @property longUrl 원본 URL
 * @property createdAt 이벤트 생성 시각
 * @property failedAt 실패 시각
 * @property retryCount 재시도 횟수
 * @property lastError 마지막 에러 메시지
 * @property status 처리 상태
 */
data class FailedEvent(
    val id: Long? = null,
    val shortUrl: ShortUrl,
    val longUrl: LongUrl,
    val createdAt: Long,
    val failedAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null,
    val status: FailedEventStatus = FailedEventStatus.PENDING
) {
    /**
     * 재시도 횟수를 증가시킨 새로운 FailedEvent를 반환합니다.
     */
    fun incrementRetry(errorMessage: String): FailedEvent {
        return copy(
            retryCount = retryCount + 1,
            lastError = errorMessage,
            failedAt = System.currentTimeMillis()
        )
    }

    /**
     * 상태를 변경한 새로운 FailedEvent를 반환합니다.
     */
    fun withStatus(newStatus: FailedEventStatus): FailedEvent {
        return copy(status = newStatus)
    }

    companion object {
        const val MAX_RETRY_COUNT = 3
    }
}

/**
 * 실패한 이벤트의 처리 상태
 */
enum class FailedEventStatus {
    /** 재처리 대기 중 */
    PENDING,
    
    /** 재처리 중 */
    PROCESSING,
    
    /** 재처리 성공 */
    RESOLVED,
    
    /** 최대 재시도 횟수 초과로 실패 */
    FAILED
}
