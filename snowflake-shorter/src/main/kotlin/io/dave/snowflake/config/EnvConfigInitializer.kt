package io.dave.snowflake.config

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import java.io.File

/**
 * 프로젝트 루트의 .env 파일을 읽어 Spring Environment에 추가하는 Initializer.
 * Spring 컨텍스트가 완전히 초기화되기 전에 실행됩니다.
 */
class EnvConfigInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val envFile = File(".env")
        if (envFile.exists()) {
            val envMap = mutableMapOf<String, Any>()
            envFile.readLines().forEach { line ->
                if (line.isNotBlank() && !line.startsWith("#")) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        envMap[parts[0].trim()] = parts[1].trim()
                    }
                }
            }

            if (envMap.isNotEmpty()) {
                applicationContext.environment.propertySources.addLast(
                    MapPropertySource("dotEnvProperties", envMap)
                )
            }
        }
    }
}
