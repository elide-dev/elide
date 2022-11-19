@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)

import Java9Modularity.configureJava9ModuleInfo

plugins {
    id("dev.elide.build")
    id("dev.elide.build.multiplatform")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
    explicitApi()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
                implementation(libs.uuid)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("stdlib-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(libs.slf4j)
                implementation(libs.jakarta.inject)
                implementation(libs.protobuf.java)
                implementation(libs.protobuf.kotlin)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json.jvm)
                implementation(libs.kotlinx.serialization.protobuf.jvm)
                implementation(libs.kotlinx.coroutines.core.jvm)
                implementation(libs.kotlinx.coroutines.jdk8)
                implementation(libs.kotlinx.coroutines.jdk9)
                implementation(libs.kotlinx.coroutines.slf4j)
                implementation(libs.kotlinx.coroutines.guava)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.logback)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(libs.kotlinx.coroutines.core.js)
                implementation(libs.kotlinx.serialization.json.js)
                implementation(libs.kotlinx.serialization.protobuf.js)
            }
        }
        val jsTest by getting

        val nativeMain by getting
        val nativeTest by getting
    }
}

configureJava9ModuleInfo(
    multiRelease = true,
)
