plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.spotless)
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Core Module Dependency (ID Generation etc.)
    api(project(":snowflake-core"))

    // Kotlin Coroutines
    api(libs.kotlinxCoroutines)

    // Logging API
    api(libs.kotlinLogging)
    testImplementation(libs.logback)

    // Serialization (Required for Domain Models if JSON mapping is needed in domain)
    implementation(libs.kotlinxSerialization)

    // Test Dependencies
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    kotlin {
        target("**/*.kt")
    }
    kotlinGradle {
        target("*.kts")
    }
}
