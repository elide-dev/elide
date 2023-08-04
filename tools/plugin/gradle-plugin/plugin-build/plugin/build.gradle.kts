/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *     https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "UnstableApiUsage",
    "UNUSED_VARIABLE",
)

import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`

    kotlin("jvm")
    kotlin("plugin.noarg")
    kotlin("plugin.allopen")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.kover")

    id("com.gradle.plugin-publish")
    alias(libs.plugins.testLogger)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.sonar)
    alias(libs.plugins.shadow)
}

val defaultJavaVersion = "17"
val defaultKotlinVersion = "1.9"

val defaultElideGroup = "dev.elide"
val elideToolsGroup = "dev.elide.tools"
val javaLanguageVersion = project.properties["versions.java.target"] as? String ?: defaultJavaVersion
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

testlogger {
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
    showExceptions = true
    showFailed = true
    showPassed = true
    showSkipped = true
    showFailedStandardStreams = true
    showFullStackTraces = true
    slowThreshold = 30000L
}

sonarqube {
    properties {
        property("sonar.projectKey", "elide-dev_buildtools")
        property("sonar.organization", "elide-dev")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.dynamicAnalysis", "reuseReports")
        property("sonar.junit.reportsPath", layout.buildDirectory.dir("reports"))
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", layout.buildDirectory.file("reports/kover/merged/xml/report.xml"))
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

    @Suppress("DEPRECATION")
    plugins {
        getByName(PluginCoordinates.ID) {
            displayName = PluginBundle.DISPLAY_NAME
        }
    }

    @Suppress("DEPRECATION")
    mavenCoordinates {
        groupId = PluginCoordinates.GROUP
        artifactId = PluginCoordinates.ID.removePrefix("$groupId.")
        version = PluginCoordinates.VERSION
    }
}

val minimumMicronaut = "3.7.8"
val preferredMicronaut = "3.10.0"
val defaultJavaMin = "17"
val defaultJavaMax = "19"
val baseJavaMin: Int = (defaultJavaMin).toInt()
val skipVersions = sortedSetOf(
    12,
    13,
    14,
    15,
    16,
    18,
)

val javaMin: Int = (
    if (System.getProperty("os.arch") == "aarch64") {
        // artificially start at java 17 for aarch64, which is the first version that supports this architecture.
        baseJavaMin
    } else {
        baseJavaMin
    }
)

val javaMax: Int = (
    if (project.hasProperty("versions.java.maximum")) {
        project.properties["versions.java.maximum"] as? String ?: defaultJavaMax
    } else {
        defaultJavaMax
    }
).toInt()

sourceSets {
    val main by getting {
        // Nothing at this time.
    }
    val test by getting {
        // Nothing at this time.
    }
}

val embedded: Configuration by configurations.creating
val implementation: Configuration by configurations.getting

configurations {
    compileClasspath.get().extendsFrom(embedded)
    runtimeClasspath.get().extendsFrom(embedded)
}

dependencies {
    api(kotlin("gradle-plugin"))
    api(libs.elide.tools.processor)
    implementation(libs.elide.base)
    implementation(libs.elide.ssg)
    implementation(libs.elide.proto.core)
    implementation(libs.elide.proto.protobuf)

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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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

tasks.compileKotlin.configure {
    kotlinOptions {
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
        jvmTarget = baseJavaMin.toString()
        javaParameters = true
        allWarningsAsErrors = true
        incremental = true
    }
}

tasks.compileTestKotlin.configure {
    kotlinOptions {
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
        jvmTarget = baseJavaMin.toString()
        javaParameters = true
        allWarningsAsErrors = true
        incremental = true
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
        jvmTarget = baseJavaMin.toString()
        javaParameters = true
        allWarningsAsErrors = true
        incremental = true
    }
}

detekt {
    source.from(files(
        "src/main/java"
    ))
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
                    layout.buildDirectory.file("reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml"),
                    layout.buildDirectory.file("reports/jacoco/testCodeCoverageReport/jacocoTestReport.xml"),
                    layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"),
                    layout.buildDirectory.file("reports/kover/xml/coverage.xml"),
                    layout.buildDirectory.file("reports/kover/xml/report.xml"),
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
    into(project.layout.buildDirectory.dir("elideJsRuntime/sources"))
}

tasks.create<Tar>("packageRuntimeAssets") {
    description = "Packages Node runtime code as an embedded tarball"
    group = "build"
    compression = Compression.GZIP

    archiveBaseName.set("js-runtime")
    archiveExtension.set("tar.gz")
    archiveVersion.set("")
    destinationDirectory.set(
        file(layout.buildDirectory.dir("resources/main/dev/elide/buildtools/js/runtime"))
    )

    dependsOn(
        tasks.named("copyNodeRuntimeAssets")
    )
    into("/") {
        from(layout.buildDirectory.dir("elideJsRuntime/sources"))
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

tasks.named("check").configure {
    dependsOn("test")
    dependsOn("detekt")
    dependsOn("ktlintCheck")
    dependsOn("koverXmlReport")
    dependsOn("koverVerify")
}

// Normal test task runs on compile JDK.
(javaMin..javaMax).filter { !skipVersions.contains(it) }.forEach { major ->
    val jdkTest = tasks.register("testJdk$major", Test::class.java) {
        description = "Runs the test suite on JDK $major"
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        javaLauncher.set(
            javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(major))
            }
        )
        val testTask = tasks.named("test", Test::class.java).get()
        classpath = testTask.classpath
        testClassesDirs = testTask.testClassesDirs
    }
    val checkTask = tasks.named("check")
    checkTask.configure {
        dependsOn(jdkTest)
    }
}

tasks.withType<Detekt>().configureEach {
    // Target version of the generated JVM bytecode. It is used for type resolution.
    jvmTarget = javaMin.toString()
}
