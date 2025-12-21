import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlinJpa)
    kotlin("kapt") // Keep kapt for annotation processing
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Core Module Dependency
    implementation(project(":snowflake-core"))

    // Spring Boot Dependencies - Core needs
    implementation(libs.spring.boot.starter.webflux) // For webflux features
    implementation(libs.spring.boot.starter.validation) // For validation
    implementation(libs.kotlinLogging) // Logging
    implementation(libs.kotlinxSerialization) // Kotlin Serialization
    implementation(libs.kotlin.reflect) // Kotlin Reflect
    implementation(libs.spring.boot.starter.data.redis) // Redis Reactive support (Lettuce included)

    // JPA & QueryDSL dependencies (REQUIRED for snowflake-app to compile and test)
    implementation(libs.spring.boot.starter.data.jpa) // Required for @Entity, @Repository etc.
    implementation(libs.mysql.connector.j) // Assuming MySQL is still used by the app
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta") // QueryDSL for JPA queries
    implementation(libs.jakarta.persistence.api) // RE-ADDED as implementation for runtime availability
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta") // QueryDSL APT processor
    kapt(libs.jakarta.annotation.api)
    kapt(libs.jakarta.persistence.api)

    // Swagger & Metric
    implementation(libs.springdoc.openapi.starter.webflux.ui) // OpenAPI documentation
    implementation(libs.spring.boot.starter.actuator) // Actuator for monitoring
    implementation(libs.micrometer.registry.prometheus)

    // Kotlin Coroutines dependencies - ensuring they are available for snowflake-app
    implementation(libs.kotlinxCoroutines) // Core coroutines library
    implementation(libs.kotlinxCoroutinesReactor) // RE-ADDED: Necessary for Spring WebFlux integration with Coroutines

    // Test Dependencies
    testImplementation(libs.spring.boot.starter.test) // Manages JUnit, Mockito, etc.
    testImplementation(libs.mockk) // Mocking framework
    testImplementation(libs.spring.mockk) // Spring integration for MockK
    testImplementation(libs.kotlinxCoroutinesTest) // Coroutines testing
    
    // H2 Database for in-memory integration testing
    testImplementation(libs.h2)
    
    // Testcontainers for integration testing
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.redis)
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
}
