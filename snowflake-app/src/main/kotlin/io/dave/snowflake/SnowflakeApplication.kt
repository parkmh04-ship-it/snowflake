package io.dave.snowflake

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class SnowflakeApplication

fun main(args: Array<String>) {
    System.setProperty("reactor.netty.ioWorkerCount", "16")
    runApplication<SnowflakeApplication>(*args)
}