package io.dave.shortener

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class UrlShortenerApplication

fun main(args: Array<String>) {
    System.setProperty("reactor.netty.ioWorkerCount", "16")
    runApplication<UrlShortenerApplication>(*args)
}