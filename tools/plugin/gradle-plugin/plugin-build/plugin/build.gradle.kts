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
    id("org.jetbrains.kotlinx.kover")

    id("java-gradle-plugin")
    id("org.sonarqube")
    id("com.gradle.plugin-publish")
    id("com.github.gmazzo.buildconfig")
    alias(libs.plugins.shadow)
}

val defaultJavaVersion = "11"
val defaultKotlinVersion = "1.7"

val defaultElideGroup = "dev.elide"
val elideToolsGroup = "dev.elide.tools"
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

sonarqube {
    properties {
        property("sonar.projectKey", "elide-dev_buildtools")
        property("sonar.organization", "elide-dev")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.dynamicAnalysis", "reuseReports")
        property("sonar.junit.reportsPath", "$buildDir/reports/")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "$buildDir/reports/kover/merged/xml/report.xml")
        property("sonar.jacoco.reportPath", "build/jacoco/test.exec")
        property("sonar.sourceEncoding", "UTF-8")
    }
}

buildConfig {
    className("ElidePluginConfig")
    packageName("dev.elide.buildtools.gradle.plugin.cfg")
    useKotlinOutput()

    // default version constant
    buildConfigField("String", "ELIDE_LIB_VERSION", "\"${libs.versions.elide.get()}\"")

    // artifact dependency config
    fun dependencyConfig(name: String, artifact: String, group: String = defaultElideGroup) {
        buildConfigField(
            "String",
            "DEPENDENCY_$name",
            "\"$group:$artifact\"",
        )
    }

    dependencyConfig("BASE", "base")
    dependencyConfig("PROTO", "proto")
    dependencyConfig("SERVER", "server")
    dependencyConfig("SSG", "ssg")
    dependencyConfig("MODEL", "model")
    dependencyConfig("TEST", "test")
    dependencyConfig("FRONTEND", "frontend")
    dependencyConfig("GRAALVM", "graalvm")
    dependencyConfig("GRAALVM_JS", "graalvm-js")
    dependencyConfig("GRAALVM_REACT", "graalvm-react")
    dependencyConfig("PLATFORM", "platform")
    dependencyConfig("CATALOG", "bom")

    dependencyConfig("PROCESSOR", "processor", elideToolsGroup)
    dependencyConfig("SUBSTRATE", "elide-substrate", elideToolsGroup)
    dependencyConfig("CONVENTION", "elide-convention-plugins", elideToolsGroup)
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

val minimumMicronaut = "3.6.3"
val preferredMicronaut = "3.6.5"

val embedded: Configuration by configurations.creating

configurations {
    compileClasspath.get().extendsFrom(embedded)
    runtimeClasspath.get().extendsFrom(embedded)
}

dependencies {
    api(kotlin("gradle-plugin"))
    api(libs.elide.tools.processor)
    implementation(libs.elide.base)
    implementation(libs.elide.ssg)

    implementation(kotlin("stdlib-jdk7"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    api(libs.plugin.node)
    implementation(libs.gradle.kotlin.dsl) {
        exclude("org.jetbrains.kotlin", "kotlin-sam-with-receiver")
    }
    implementation(libs.kotlin.samWithReceiver)

    api("io.micronaut.gradle:micronaut-gradle-plugin") {
        version {
            strictly("[$minimumMicronaut, $preferredMicronaut]")
            prefer(preferredMicronaut)
        }
    }

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
    explicitApi()

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
        allWarningsAsErrors = true
        incremental = true
    }
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

subprojects {
    apply {
        plugin("org.jetbrains.kotlinx.kover")
        plugin("org.sonarqube")
    }

    sonarqube {
        properties {
            property("sonar.sources", "src/main/java")
            property("sonar.tests", "src/test/java")
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                listOf(
                    "$buildDir/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml",
                    "$buildDir/reports/jacoco/testCodeCoverageReport/jacocoTestReport.xml",
                    "$buildDir/reports/jacoco/test/jacocoTestReport.xml",
                    "$buildDir/reports/kover/xml/coverage.xml",
                )
            )
        }
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
