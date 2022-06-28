@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.konan.target.HostManager
import java.net.URI

val jakartaVersion = project.properties["versions.jakarta-inject"] as String
val protobufVersion = project.properties["versions.protobuf"] as String
val protobufTypesVersion = project.properties["versions.protobufTypes"] as String
val slf4jVersion = project.properties["versions.slf4j"] as String
val javaLanguageVersion = project.properties["versions.java.language"] as String
val kotlinSdkVersion = project.properties["versions.kotlin.sdk"] as String
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String
val grpcVersion = project.properties["versions.grpc"] as String
val kotlinxAtomicFuVersion = project.properties["versions.kotlinx.atomicfu"] as String
val kotlinxCoroutinesVersion = project.properties["versions.kotlinx.coroutines"] as String
val kotlinxCollectionsVersion = project.properties["versions.kotlinx.collections"] as String
val kotlinxDatetimeVersion = project.properties["versions.kotlinx.datetime"] as String
val kotlinxSerializationVersion = project.properties["versions.kotlinx.serialization"] as String
val junitJupiterVersion =  project.properties["versions.junit.jupiter"] as String
val logbackVersion = project.properties["versions.logback"] as String

plugins {
    `maven-publish`
    signing
    kotlin("multiplatform")
    kotlin("plugin.atomicfu")
    kotlin("plugin.serialization")
    id("com.adarshr.test-logger")
    id("com.google.cloud.artifactregistry.gradle-plugin")
    id("org.jetbrains.dokka")
    id("org.sonarqube")
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
        compilations.all {
            kotlinOptions {
                apiVersion = kotlinLanguageVersion
                languageVersion = kotlinLanguageVersion
            }
        }
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
    targets {
        if (HostManager.hostIsMac) {
            macosX64()
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
        }
    }

    publishing {
        publications {}
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
                implementation("org.jetbrains.kotlinx:atomicfu:$kotlinxAtomicFuVersion")
                implementation("com.benasher44:uuid:${Versions.kotlinUuid}")
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
                api("org.slf4j:slf4j-api:$slf4jVersion")
                implementation("jakarta.inject:jakarta.inject-api:$jakartaVersion")
                implementation("com.google.protobuf:protobuf-java:$protobufVersion")
                implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinxCoroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk9:$kotlinxCoroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinxCoroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:$kotlinxCoroutinesVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("test-junit5"))
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
                runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
            }
        }
        val jsMain by getting {
            kotlin.srcDir("src/nonJvmMain/kotlin")
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinxCoroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-js:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-js:$kotlinxSerializationVersion")

                implementation(npm("uuid", Versions.jsUuid))
                implementation(npm("@types/uuid", Versions.jsUuidTypes))
            }
        }
        val jsTest by getting

        val nix64MainSourceDirs = listOf(
            "src/nonJvmMain/kotlin",
            "src/nativeMain/kotlin",
            "src/nix64Main/kotlin"
        )

        val nix32MainSourceDirs = listOf(
            "src/nonJvmMain/kotlin",
            "src/nativeMain/kotlin",
            "src/nix32Main/kotlin"
        )

        if (HostManager.hostIsMac) {
            val appleMain32SourceDirs = listOf(
                "src/appleMain/kotlin"
            ) + nix32MainSourceDirs

            val appleMain64SourceDirs = listOf(
                "src/appleMain/kotlin"
            ) + nix64MainSourceDirs

            val macosX64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val macosX64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val macosArm64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val macosArm64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val iosArm64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val iosArm64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val iosArm32Main by getting { kotlin.srcDirs(appleMain32SourceDirs) }
            val iosArm32Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val iosX64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val iosX64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val iosSimulatorArm64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val iosSimulatorArm64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val watchosArm32Main by getting { kotlin.srcDirs(appleMain32SourceDirs) }
            val watchosArm32Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val watchosArm64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val watchosArm64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val watchosX64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val watchosX64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val watchosX86Main by getting { kotlin.srcDirs(appleMain32SourceDirs) }
            val watchosX86Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val watchosSimulatorArm64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val watchosSimulatorArm64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val tvosArm64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val tvosArm64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val tvosX64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val tvosX64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
            val tvosSimulatorArm64Main by getting { kotlin.srcDirs(appleMain64SourceDirs) }
            val tvosSimulatorArm64Test by getting { kotlin.srcDir("src/appleTest/kotlin") }
        }
        if (HostManager.hostIsMingw || HostManager.hostIsMac) {
            val mingwX64Main by getting {
                kotlin.srcDirs(
                    listOf(
                        "src/nonJvmMain/kotlin",
                        "src/nativeMain/kotlin",
                        "src/mingwMain/kotlin"
                    )
                )
            }
            val mingwX64Test by getting {
                kotlin.srcDir("src/mingwTest/kotlin")
            }
        }
        if (HostManager.hostIsLinux || HostManager.hostIsMac) {
            val linuxX64Main by getting { kotlin.srcDirs(nix64MainSourceDirs) }
        }

//        val nativeMain by getting
//        val nativeTest by getting
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon>().configureEach {
    kotlinOptions {
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
        jvmTarget = javaLanguageVersion
        javaParameters = true
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    kotlinOptions {
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
    }
}
