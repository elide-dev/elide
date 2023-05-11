@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)
@file:OptIn(
    ExperimentalWasmDsl::class,
)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    id("dev.elide.build")
    id("dev.elide.build.multiplatform.core")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
    explicitApi()
    jvmToolchain(Elide.javaTargetMinimum)

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
    }

    wasm {
        browser {
            testTask {
                useKarma {
                    this.webpackConfig.experiments.add("topLevelAwait")
                    useChromeHeadless()
                    useConfigDirectory(project.projectDir.resolve("karma.config.d").resolve("wasm"))
                }
            }
        }
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
                api(libs.elide.uuid)
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
                api(libs.elide.uuid)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.collections.immutable)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.coroutines.core.jvm)
                api(libs.kotlinx.coroutines.jdk9)
                api(libs.kotlinx.coroutines.slf4j)
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

                api(kotlin("stdlib-js"))
                api(libs.kotlinx.coroutines.core.js)
                api(libs.kotlinx.serialization.json.js)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.collections.immutable)
                api(libs.elide.uuid)
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
                api(kotlin("stdlib"))
                api(libs.elide.uuid)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.collections.immutable)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.coroutines.core)
            }
        }
        val nativeTest by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("test"))
            }
        }
        val wasmMain by getting {
            //
        }
        val wasmTest by getting {
            //
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

// temp: disable WASM tests
tasks.named("wasmTest") {
    enabled = false
}
tasks.named("wasmBrowserTest") {
    enabled = false
}

val buildDocs = project.properties["buildDocs"] == "true"
val javadocJar: TaskProvider<Jar>? = if (buildDocs) {
    val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(dokkaHtml.outputDirectory)
    }
    javadocJar
} else null

publishing {
    publications.withType<MavenPublication> {
        if (buildDocs) {
            artifact(javadocJar)
        }
        artifactId = artifactId.replace("base", "elide-base")

        pom {
            name.set("Elide Base")
            url.set("https://elide.dev")
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
                url.set("https://github.com/elide-dev/elide")
            }
        }
    }
}

afterEvaluate {
    val jvmCompileTasks = listOf(
        "compileKotlinJvmJava11",
        "compileKotlinJvmJava17",
        "compileKotlinJvmJava19",
    ).mapNotNull {
        try {
            tasks.named(it)
        } catch (err: Throwable) {
            null
        }
    }

    val jvmTestTasks = listOf(
        "compileTestKotlinJvmJava11",
        "compileTestKotlinJvmJava17",
        "compileTestKotlinJvmJava19",
    ).mapNotNull {
        try {
            tasks.named(it)
        } catch (err: Throwable) {
            null
        }
    }

    listOf(
        "jvmTest",
        "jvmApiBuild",
        "compileJava",
        "koverGenerateArtifact",
    ).forEach {
        try {
            tasks.named(it).configure {
                dependsOn(jvmCompileTasks)
            }
        } catch (err: Throwable) {
            // ignore
        }
    }

    listOf(
        "jvmTest",
    ).forEach {
        try {
            tasks.named(it).configure {
                dependsOn(jvmTestTasks)
            }
        } catch (err: Throwable) {
            // ignore
        }
    }

    val signingTasks = listOf(
        "signJvmPublication",
        "signJsPublication",
        "signKotlinMultiplatformPublication",
        "signNativePublication",
        "signIosArm64Publication",
        "signIosX64Publication",
        "signMacosArm64Publication",
        "signWasmPublication",
        "signMingwX64Publication",
        "signTvosArm64Publication",
        "signTvosX64Publication",
        "signWatchosX64Publication",
        "signWatchosArm32Publication",
        "signWatchosArm64Publication",
    ).mapNotNull {
        try {
            tasks.named(it)
        } catch (err: Throwable) {
            null
        }
    }

    listOf(
        "publishJsPublicationToElideRepository",
        "publishJvmPublicationToElideRepository",
        "publishKotlinMultiplatformPublicationToElideRepository",
        "publishNativePublicationToElideRepository",
        "publishIosArm64PublicationToElideRepository",
        "publishIosX64PublicationToElideRepository",
        "publishMacosArm64PublicationToElideRepository",
        "publishWasmPublicationToElideRepository",
        "publishMingwX64PublicationToElideRepository",
        "publishTvosArm64PublicationToElideRepository",
        "publishTvosX64PublicationToElideRepository",
        "publishWatchosArm32PublicationToElideRepository",
        "publishWatchosArm64PublicationToElideRepository",
        "publishWatchosX64PublicationToElideRepository",
    ).forEach {
        try {
            tasks.named(it).configure {
                dependsOn(signingTasks)
            }
        } catch (err: Throwable) {
            // ignore
        }
    }
}
