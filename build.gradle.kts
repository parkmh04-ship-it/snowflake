plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.springBoot) apply false
    alias(libs.plugins.kotlinSpring) apply false
    alias(libs.plugins.spotless) apply false
}

subprojects {
    repositories {
        mavenCentral()
    }
}
