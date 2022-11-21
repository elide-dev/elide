@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "UnstableApiUsage"
)

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.noarg")
    kotlin("plugin.allopen")
    kotlin("plugin.serialization")

    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.github.gmazzo.buildconfig")
    alias(libs.plugins.shadow)
}

val defaultJavaVersion = "11"
val defaultKotlinVersion = "1.7"

val javaLanguageVersion = project.properties["versions.java.language"] as? String ?: defaultJavaVersion
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as? String ?: defaultKotlinVersion

gradlePlugin {
    plugins {
        create(PluginCoordinates.ID) {
            id = PluginCoordinates.ID
            implementationClass = PluginCoordinates.IMPLEMENTATION_CLASS
            version = PluginCoordinates.VERSION
        }
    }
}

buildConfig {
    className("ElidePluginConfig")
    packageName("dev.elide.buildtools.gradle.plugin.cfg")
    useKotlinOutput()

    buildConfigField("String", "ELIDE_LIB_VERSION", "\"${libs.versions.elide.get()}\"")
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
    website = PluginBundle.WEBSITE
    vcsUrl = PluginBundle.VCS
    description = PluginBundle.DESCRIPTION
    tags = PluginBundle.TAGS

    plugins {
        getByName(PluginCoordinates.ID) {
            displayName = PluginBundle.DISPLAY_NAME
        }
    }

    mavenCoordinates {
        groupId = PluginCoordinates.GROUP
        artifactId = PluginCoordinates.ID.removePrefix("$groupId.")
        version = PluginCoordinates.VERSION
    }
}

repositories {
    google()
    mavenCentral()
}

val embedded: Configuration by configurations.creating

configurations {
    compileClasspath.get().extendsFrom(embedded)
    runtimeClasspath.get().extendsFrom(embedded)
}

dependencies {
    api(kotlin("gradle-plugin"))
    api(libs.elide.base)
    api(libs.elide.ssg)

    implementation(kotlin("stdlib-jdk7"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    api(libs.plugin.node)
    implementation(libs.gradle.kotlin.dsl) {
        exclude("org.jetbrains.kotlin", "kotlin-sam-with-receiver")
    }
    implementation(libs.kotlin.samWithReceiver)

    // KotlinX
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.core.jvm)
    api(libs.kotlinx.coroutines.jdk8)
    api(libs.kotlinx.coroutines.jdk9)
    api(libs.kotlinx.coroutines.guava)
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.serialization.protobuf)

    // Elide: Kotlin Plugins
    implementation(libs.elide.kotlin.plugin.redakt)

    // Elide: Embedded Libs
    embedded(libs.elide.base)
    embedded(libs.elide.ssg)
    embedded(libs.elide.proto)

    // Elide: Embedded Tools
    embedded(libs.closure.templates)
    embedded(libs.closure.compiler)
    embedded(libs.closure.stylesheets)
    embedded(libs.brotli)
    embedded(libs.brotli.native.osx)
    embedded(libs.brotli.native.linux)
    embedded(libs.brotli.native.windows)

    // Common Dependencies
    api(libs.protobuf.java)
    api(libs.protobuf.util)
    api(libs.protobuf.kotlin)
    api(libs.google.api.common)
    api(libs.google.common.html.types.proto)
    api(libs.google.common.html.types.types)

    // General Implementation
    api(libs.slf4j)
    api(libs.gson)
    implementation(libs.picocli)
    implementation(libs.picocli.codegen)
    implementation(libs.checker)
    implementation(libs.commons.compress)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.truth.proto)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    sourceSets.all {
        languageSettings.apply {
            apiVersion = kotlinLanguageVersion
            languageVersion = kotlinLanguageVersion
            progressiveMode = true
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        apiVersion = Elide.kotlinLanguage
        languageVersion = Elide.kotlinLanguage
        jvmTarget = "11"
        javaParameters = true
        freeCompilerArgs = Elide.kaptCompilerArgs
        allWarningsAsErrors = false
        incremental = true
    }
}

sourceSets.getByName("main").java {
    srcDir("src/model/java")
    srcDir("src/model/kotlin")
}

detekt {
    source = files(
        "src/main/java"
    )
}

ktlint {
    filter {
        exclude("**/model/**")
    }
}

tasks {
    shadowJar {
        configurations = listOf(
            embedded
        )
    }
}

tasks.create<Copy>("copyNodeRuntimeAssets") {
    description = "Copy runtime Node assets to build root"
    group = "build"
    from("${project.projectDir}/src/main/node/runtime") {
        include("**/*.js")
        include("**/*.json")
    }
    into("${project.buildDir}/elideJsRuntime/sources")
}

tasks.create<Tar>("packageRuntimeAssets") {
    description = "Packages Node runtime code as an embedded tarball"
    group = "build"
    compression = Compression.GZIP

    archiveBaseName.set("js-runtime")
    archiveExtension.set("tar.gz")
    archiveVersion.set("")
    destinationDirectory.set(
        file("${project.buildDir}/resources/main/dev/elide/buildtools/js/runtime")
    )

    dependsOn(
        tasks.named("copyNodeRuntimeAssets")
    )
    into("/") {
        from("${project.buildDir}/elideJsRuntime/sources")
        include(
            "**/*.json",
            "**/*.js"
        )
    }
}

tasks.create("setupPluginUploadFromEnvironment") {
    doLast {
        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}

@Suppress("UnstableApiUsage")
tasks.named<ProcessResources>("processResources") {
    dependsOn(
        tasks.named("packageRuntimeAssets")
    )
}
