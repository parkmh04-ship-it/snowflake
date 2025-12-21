plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.spotless)
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

    // Test Dependencies (Pure Kotlin/JUnit5)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
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


spotless {
    kotlin {
        target("**/*.kt")
    }
    kotlinGradle {
        target("*.kts")
    }
}
