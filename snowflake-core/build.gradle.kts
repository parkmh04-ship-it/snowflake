plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.springBoot) // ✅ [유지] BOM 및 테스트 편의를 위해 유지
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.spotless)
    kotlin("kapt") // Keep kapt for annotation processing
    jacoco // Keep jacoco plugin
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Kotlin Compile options
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {

    // Kotlin Coroutines (essential for suspend functions)
    api(libs.kotlinxCoroutines) // Use catalog alias

    // Logging (lightweight and standard for libraries)
    api(libs.kotlinLogging) // Use catalog alias

    // Spring Boot basic starter
    api(libs.spring.boot.starter) // Use catalog alias
    // JPA & QueryDSL dependencies (REQUIRED for snowflake-app to compile and test)
    api(libs.spring.boot.starter.data.jpa) // Required for @Entity, @Repository etc.
    api(libs.mysql.connector.j) // Assuming MySQL is still used by the app
    api("com.querydsl:querydsl-jpa:5.1.0:jakarta") // QueryDSL for JPA queries
    api(libs.jakarta.persistence.api) // RE-ADDED as implementation for runtime availability
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta") // QueryDSL APT processor
    kapt(libs.jakarta.annotation.api)
    kapt(libs.jakarta.persistence.api)

    // Micrometer Core API
    api(libs.micrometer.core) // Use catalog alias

    // Test Dependencies
    testApi(libs.spring.boot.starter.test) // Use catalog alias
    testApi(libs.mockk) // Use catalog alias
    testApi(libs.spring.mockk) // Use catalog alias
    testApi(libs.kotlinxCoroutinesTest) // Use catalog alias
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
}

// Disable bootJar task as this is a library, not an executable application
tasks.named("bootJar") {
    enabled = false
}

spotless {
    kotlin {
        target("**/*.kt")
    }
    kotlinGradle {
        target("*.kts")
    }
}
