package io.dave.snowflake.id

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class SnowflakeIdApplication

fun main(args: Array<String>) {
    System.setProperty("reactor.netty.ioWorkerCount", "16")
    runApplication<SnowflakeIdApplication>(*args)
}