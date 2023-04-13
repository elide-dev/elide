@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)

import org.jetbrains.kotlin.konan.target.HostManager
import Java9Modularity.configureJava9ModuleInfo

plugins {
    id("dev.elide.build")
    id("dev.elide.build.multiplatform")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
    explicitApi()

    jvm {
        withJava()
    }

    js(IR) {
        binaries.executable()
    }

    macosArm64()
    iosArm64()
    iosX64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    tvosArm64()
    tvosX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                api(project(":packages:core"))
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.collections.immutable)
                api(libs.kotlinx.datetime)
                implementation(libs.uuid)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("stdlib"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                api(libs.slf4j)
                api(libs.jakarta.inject)
                api(libs.micronaut.inject.java)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.coroutines.core.jvm)
                implementation(libs.kotlinx.coroutines.jdk9)
                implementation(libs.kotlinx.coroutines.slf4j)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.logback)
            }
        }
        val jsMain by getting {
            dependencies {
                // KT-57235: fix for atomicfu-runtime error
                api("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:1.8.20-RC")

                implementation(kotlin("stdlib-js"))
                implementation(libs.kotlinx.coroutines.core.js)
                implementation(libs.kotlinx.serialization.json.js)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(kotlin("test"))
            }
        }
        val nativeMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
        val nativeTest by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("test"))
            }
        }

        val mingwX64Main by getting { dependsOn(nativeMain) }
        val macosArm64Main by getting { dependsOn(nativeMain) }
        val iosArm64Main by getting { dependsOn(nativeMain) }
        val iosX64Main by getting { dependsOn(nativeMain) }
        val watchosArm32Main by getting { dependsOn(nativeMain) }
        val watchosArm64Main by getting { dependsOn(nativeMain) }
        val watchosX64Main by getting { dependsOn(nativeMain) }
        val tvosArm64Main by getting { dependsOn(nativeMain) }
        val tvosX64Main by getting { dependsOn(nativeMain) }
    }
}

configureJava9ModuleInfo(
    multiRelease = true,
)

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

publishing {
    publications.withType<MavenPublication> {
        artifact(javadocJar)
        artifactId = artifactId.replace("base", "elide-base")

        pom {
            name.set("Elide Base")
            url.set("https://github.com/elide-dev/elide")
            description.set(
                "Baseline logic and utilities which are provided for most supported Kotlin and Elide platforms."
            )

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/elide-dev/elide/blob/v3/LICENSE")
                }
            }
            developers {
                developer {
                    id.set("sgammon")
                    name.set("Sam Gammon")
                    email.set("samuel.gammon@gmail.com")
                }
            }
            scm {
                url.set("https://github.com/elide-dev/v3")
            }
        }
    }
}
