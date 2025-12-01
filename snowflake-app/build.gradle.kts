plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.spotless)
    kotlin("kapt")
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Spring Boot Dependencies
    implementation(libs.kotlinxCoroutinesReactor)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.kotlinLogging)
    implementation(libs.kotlinxSerialization)
    implementation(libs.kotlin.reflect)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.kaffeine)
    
    // JPA & QueryDSL
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.mysql.connector.j)
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api:2.1.1")
    kapt("jakarta.persistence:jakarta.persistence-api:3.1.0")

    implementation(libs.springdoc.openapi.starter.webflux.ui)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)

    // Test Dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
    testImplementation(libs.spring.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
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

spotless {
    kotlin {
        target("**/*.kt")
    }
    kotlinGradle {
        target("*.kts")
    }
}