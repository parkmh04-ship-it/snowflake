package io.dave.shortener.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "snowflake")
data class SnowflakeProperties(
    /**
     * 애플리케이션 시작 시 할당받을 Snowflake 워커 ID의 개수입니다.
     */
    val workerThreadCount: Int
)
