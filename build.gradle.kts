import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = libs.versions.kotlin.get()
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion

    alias(libs.plugins.kotlinx.benchmark)
}

group = "io.github.sfuri"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.logback.classic)
    implementation(libs.kotlinx.benchamrk.runtime)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xcontext-parameters"))
    }
}
