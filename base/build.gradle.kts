@file:Suppress("UNUSED_VARIABLE")

val jakartaVersion = project.properties["versions.jakarta-inject"] as String
val protobufVersion = project.properties["versions.protobuf"] as String
val protobufTypesVersion = project.properties["versions.protobufTypes"] as String
val slf4jVersion = project.properties["versions.slf4j"] as String
val javaLanguageVersion = project.properties["versions.java.language"] as String
val kotlinSdkVersion = project.properties["versions.kotlin.sdk"] as String
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String
val grpcVersion = project.properties["versions.grpc"] as String
val grpcKotlinVersion = project.properties["versions.grpcKotlin"] as String
val grpcWebVersion = project.properties["versions.grpcWeb"] as String
val micronautVersion = project.properties["versions.micronaut"] as String
val micronautPluginVersion = project.properties["versions.micronautPlugin"] as String
val kotlinxCoroutinesVersion = project.properties["versions.kotlinx.coroutines"] as String
val kotlinxCollectionsVersion = project.properties["versions.kotlinx.collections"] as String
val kotlinxDatetimeVersion = project.properties["versions.kotlinx.datetime"] as String
val kotlinxSerializationVersion = project.properties["versions.kotlinx.serialization"] as String

plugins {
    `maven-publish`
    signing
    kotlin("multiplatform")
    kotlin("plugin.atomicfu")
    kotlin("plugin.serialization")
    id("com.google.cloud.artifactregistry.gradle-plugin")
}

group = "dev.elide"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    maven("gcs://elide-snapshots/repository/v3")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven("gcs://elide-snapshots/repository/v3")
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
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
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
                implementation("org.jetbrains.kotlin:atomicfu:1.6.21")  // bugfix for missing dep
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
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
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinxCoroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-js:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-js:$kotlinxSerializationVersion")
            }
        }
        val jsTest by getting
        val nativeMain by getting
        val nativeTest by getting
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
