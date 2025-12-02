package io.dave.snowflake.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "snowflake")
data class SnowflakeProperties(
    /**
     * 애플리케이션 시작 시 할당받을 Snowflake 워커 ID의 개수입니다.
     */
    val workerThreadCount: Int
)
