package io.dave.snowflake.integration.config

import io.dave.snowflake.config.AssignedWorkerInfo
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestSnowflakeConfig {

    @Bean
    fun assignedWorkerInfo(): AssignedWorkerInfo {
        return AssignedWorkerInfo(instanceId = "testInstance", listOf(1, 2, 3, 4))
    }
}
