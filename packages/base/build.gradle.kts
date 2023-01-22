@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)

import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.konan.target.HostManager
import Java9Modularity.configureJava9ModuleInfo

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.kover")
    id("dev.elide.build.core")
    id("dev.elide.build")
}

group = "dev.elide"
version = rootProject.version as String

val defaultJavaVersion = "11"
val defaultKotlinVersion = "1.8"

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as? String ?: defaultJavaVersion
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as? String ?: defaultKotlinVersion
val strictMode = project.properties["strictMode"] as? String == "true"
val enableK2 = project.properties["elide.kotlin.k2"] as? String == "true"

kotlin {
    targets {
        jvm {
            withJava()
        }

        js(IR) {
            compilations.all {
                kotlinOptions {
                    sourceMap = true
                    moduleKind = "umd"
                    metaInfo = true
                }
            }
            browser()
            nodejs()
        }

        if (HostManager.hostIsMac) {
            // @TODO(sgammon): breaks build
            // macosX64()
            wasm32()
            macosArm64()
            iosX64()
            iosArm64()
            iosArm32()
            iosSimulatorArm64()
            watchosArm32()
            watchosArm64()
            watchosX86()
            watchosX64()
            watchosSimulatorArm64()
            tvosArm64()
            tvosX64()
            tvosSimulatorArm64()
        }
        if (HostManager.hostIsMingw || HostManager.hostIsMac) {
            mingwX64 {
                binaries.findTest(DEBUG)!!.linkerOpts = mutableListOf("-Wl,--subsystem,windows")
            }
        }
        if (HostManager.hostIsLinux || HostManager.hostIsMac) {
            linuxX64()
            linuxArm32Hfp()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                api(project(":packages:core"))
                api(libs.kotlinx.serialization.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("stdlib"))
            }
        }
        val nonJvmMain by creating { dependsOn(commonMain) }
        val nonJvmTest by creating { dependsOn(commonTest) }
        val jsMain by getting {
            dependsOn(nonJvmMain)

            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(libs.kotlinx.serialization.json.js)
            }
        }
        val jsTest by getting {
            dependsOn(nonJvmTest)

            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(kotlin("test"))
            }
        }

        val nativeMain by creating { dependsOn(nonJvmMain) }
        val nativeTest by creating { dependsOn(nonJvmTest) }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                api(libs.slf4j)
                api(libs.jakarta.inject)
                api(libs.micronaut.inject.java)
                implementation(libs.kotlinx.serialization.core)
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

        val nix64Main by creating { dependsOn(nativeMain) }
        val nix64Test by creating { dependsOn(nativeTest) }
        val nix32Main by creating { dependsOn(nativeMain) }
        val nix32Test by creating { dependsOn(nativeTest) }

        if (HostManager.hostIsMac) {
            val appleMain by creating { dependsOn(nativeMain) }
            val appleTest by creating { dependsOn(nativeTest) }
            val apple64Main by creating {
                dependsOn(appleMain)
                dependsOn(nix64Main)
            }
            val apple64Test by creating {
                dependsOn(appleTest)
                dependsOn(nix64Test)
            }
            val apple32Main by creating {
                dependsOn(appleMain)
                dependsOn(nix32Main)
            }
            val apple32Test by creating {
                dependsOn(appleTest)
                dependsOn(nix32Test)
            }
            val iosX64Main by getting { dependsOn(apple64Main) }
            val iosX64Test by getting { dependsOn(apple64Test) }
            val iosArm64Main by getting { dependsOn(apple64Main) }
            val iosArm64Test by getting { dependsOn(apple64Test) }
            // @TODO(sgammon): breaks build
            // val macosX64Main by getting { dependsOn(apple64Main) }
            // val macosX64Test by getting { dependsOn(apple64Test) }
            val macosArm64Main by getting { dependsOn(apple64Main) }
            val macosArm64Test by getting { dependsOn(apple64Test) }
            val iosArm32Main by getting { dependsOn(apple32Main) }
            val iosArm32Test by getting { dependsOn(apple32Test) }
            val iosSimulatorArm64Main by getting { dependsOn(apple64Main) }
            val iosSimulatorArm64Test by getting { dependsOn(apple64Test) }
            val watchosArm32Main by getting { dependsOn(apple32Main) }
            val watchosArm32Test by getting { dependsOn(apple32Test) }
            val watchosArm64Main by getting { dependsOn(apple64Main) }
            val watchosArm64Test by getting { dependsOn(apple64Test) }
            val watchosX64Main by getting { dependsOn(apple64Main) }
            val watchosX64Test by getting { dependsOn(apple64Test) }
            val watchosX86Main by getting { dependsOn(apple32Main) }
            val watchosX86Test by getting { dependsOn(apple32Test) }
            val watchosSimulatorArm64Main by getting { dependsOn(apple64Main) }
            val watchosSimulatorArm64Test by getting { dependsOn(apple64Test) }
            val tvosArm64Main by getting { dependsOn(apple64Main) }
            val tvosArm64Test by getting { dependsOn(apple64Test) }
            val tvosX64Main by getting { dependsOn(apple64Main) }
            val tvosX64Test by getting { dependsOn(apple64Test) }
            val tvosSimulatorArm64Main by getting { dependsOn(apple64Main) }
            val tvosSimulatorArm64Test by getting { dependsOn(apple64Test) }
        }

        if (HostManager.hostIsMingw || HostManager.hostIsMac) {
            val mingwMain by creating { dependsOn(nativeMain) }
            val mingwTest by creating { dependsOn(nativeTest) }
            val mingwX64Main by getting { dependsOn(mingwMain) }
            val mingwX64Test by getting { dependsOn(mingwTest) }
        }

        if (HostManager.hostIsLinux || HostManager.hostIsMac) {
            val linuxX64Main by getting { dependsOn(nix64Main) }
            val linuxX64Test by getting { dependsOn(nix64Test) }
            val linuxArm32HfpMain by getting { dependsOn(nix32Main) }
            val linuxArm32HfpTest by getting { dependsOn(nix32Test) }
        }
    }
}

kotlin {
    explicitApi()
    targets.all {
        compilations.all {
            // https://youtrack.jetbrains.com/issue/KT-46257
            kotlinOptions.allWarningsAsErrors = HostManager.hostIsMac
        }
    }
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets.all {
        languageSettings.apply {
            apiVersion = kotlinLanguageVersion
            languageVersion = kotlinLanguageVersion
            progressiveMode = true
            optIn("kotlin.ExperimentalUnsignedTypes")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = javaLanguageTarget
    targetCompatibility = javaLanguageTarget
    options.isFork = true
    options.isIncremental = true
}

tasks.withType<KotlinCompileCommon>().configureEach {
    kotlinOptions {
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
        freeCompilerArgs = Elide.mppCompilerArgs
        allWarningsAsErrors = strictMode
        useK2 = enableK2
        incremental = true
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
        jvmTarget = javaLanguageTarget
        freeCompilerArgs = Elide.mppCompilerArgs
        javaParameters = true
        allWarningsAsErrors = strictMode
        useK2 = enableK2
        incremental = true
    }
}

val projectDirGenRoot = "$buildDir/generated/projectdir/kotlin"
val generateProjDirValTask = tasks.register("generateProjectDirectoryVal") {
    doLast {
        mkdir(projectDirGenRoot)
        val projDirFile = File("$projectDirGenRoot/projdir.kt")
        projDirFile.writeText("")
        projDirFile.appendText(
            // language=kotlin
            """
                |package elide.util.uuid
                |
                |import kotlin.native.concurrent.SharedImmutable
                |
                |@SharedImmutable
                |internal const val PROJECT_DIR_ROOT = ""${'"'}${projectDir.absolutePath}""${'"'}
                |
            """.trimMargin()
        )
    }
}

kotlin.sourceSets.named("commonTest") {
    this.kotlin.srcDir(projectDirGenRoot)
}

// Ensure this runs before any test compile task
tasks.withType<AbstractCompile>().configureEach {
    if (name.toLowerCase().contains("test")) {
        dependsOn(generateProjDirValTask)
    }
}

tasks.withType<AbstractKotlinCompileTool<*>>().configureEach {
    if (name.toLowerCase().contains("test")) {
        dependsOn(generateProjDirValTask)
    }
}

configureJava9ModuleInfo(
    multiRelease = false,
)
