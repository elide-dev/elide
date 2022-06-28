@file:Suppress(
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)

import java.net.URI

plugins {
    `maven-publish`
    signing
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.testLogger)
    alias(libs.plugins.dokka)
    alias(libs.plugins.sonar)
}

group = "dev.elide"
version = rootProject.version as String

repositories {
    google()
    mavenCentral()
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    maven(project.properties["elide.publish.repo.maven"] as String)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven {
            name = "elide"
            url = URI.create(project.properties["elide.publish.repo.maven"] as String)

            if (project.hasProperty("elide.publish.repo.maven.auth")) {
                credentials {
                    username = (project.properties["elide.publish.repo.maven.username"] as? String
                        ?: System.getenv("PUBLISH_USER"))?.ifBlank { null }
                    password = (project.properties["elide.publish.repo.maven.password"] as? String
                        ?: System.getenv("PUBLISH_TOKEN"))?.ifBlank { null }
                }
            }
        }
    }

    publications.withType<MavenPublication> {
        artifact(javadocJar.get())
        pom {
            name.set("Elide")
            description.set("Polyglot application framework")
            url.set("https://github.com/elide-dev/v3")

            licenses {
                license {
                    name.set("Properity License")
                    url.set("https://github.com/elide-dev/v3/blob/v3/LICENSE")
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

kotlin {
    explicitApi()

    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(BOTH) {
        compilations.all {
            kotlinOptions {
                sourceMap = true
                moduleKind = "umd"
                metaInfo = true
            }
        }
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }

    val publicationsFromMainHost =
        listOf(jvm(), js()).map { it.name } + "kotlinMultiplatform"
    publishing {
        publications {
            matching { it.name in publicationsFromMainHost }.all {
                val targetPublication = this@all
                tasks.withType<AbstractPublishToMaven>()
                    .matching { it.publication == targetPublication }
                    .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
            }
        }
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.language.get()
            apiVersion = libs.versions.kotlin.language.get()
            optIn("kotlin.ExperimentalUnsignedTypes")
            progressiveMode = true
        }
    }

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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon>().configureEach {
    kotlinOptions {
        apiVersion = libs.versions.kotlin.language.get()
        languageVersion = libs.versions.kotlin.language.get()
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        apiVersion = libs.versions.kotlin.language.get()
        languageVersion = libs.versions.kotlin.language.get()
        jvmTarget = libs.versions.java.get()
        javaParameters = true
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    kotlinOptions {
        apiVersion = libs.versions.kotlin.language.get()
        languageVersion = libs.versions.kotlin.language.get()
    }
}
