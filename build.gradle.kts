
plugins {
    val kotlinVersion = libs.versions.kotlin.get()
    kotlin("multiplatform") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}

group = "io.github.sfuri"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        verbose = true
        freeCompilerArgs.set(listOf(
            "-Xcontext-parameters",
            "-Xexpect-actual-classes",
        ))
    }

    jvm()
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(libs.kotlinx.benchamrk.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.pedestal.weak)
                implementation(libs.kermit.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.bundles.test.kotest)
            }
        }

        jvmTest {
            dependencies { }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}
