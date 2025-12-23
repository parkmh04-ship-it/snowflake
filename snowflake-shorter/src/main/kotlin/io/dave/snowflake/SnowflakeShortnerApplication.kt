package io.dave.snowflake

import io.dave.snowflake.config.EnvConfigInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class SnowflakeApplication

fun main(args: Array<String>) {
    System.setProperty("reactor.netty.ioWorkerCount", "16")

    SpringApplicationBuilder(SnowflakeApplication::class.java)
        .initializers(EnvConfigInitializer())
        .run(*args)
}