@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "DSL_SCOPE_VIOLATION",
)

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import java.util.Properties

plugins {
    java
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.sonar)
    alias(libs.plugins.versionCheck)
}

// Set version from `.version` if stamping is enabled.
version = if (project.hasProperty("elide.stamp") && project.properties["elide.stamp"] == "true") {
    file(".version").readText().trim().replace("\n", "").ifBlank {
        throw IllegalStateException("Failed to load `.version`")
    }
} else {
    "1.0-SNAPSHOT"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val props = Properties()
val overlay = file(
    if (project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true") {
        "gradle-ci.properties"
    } else {
        "local.properties"
    }
)

if (overlay.exists()) props.load(overlay.inputStream())
val isCI = project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true"

sonarqube {
    properties {
        property("sonar.projectKey", "elide-dev_buildtools")
        property("sonar.organization", "elide-dev")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.dynamicAnalysis", "reuseReports")
        property("sonar.junit.reportsPath", "build/reports/")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "$buildDir/reports/kover/merged/xml/report.xml")
        property("sonar.jacoco.reportPath", "build/jacoco/test.exec")
        property("sonar.sourceEncoding", "UTF-8")
    }
}

koverMerged {
    enable()

    xmlReport {
        onCheck.set(isCI)
    }

    htmlReport {
        onCheck.set(isCI)
    }
}

subprojects {
    apply {
        plugin("io.gitlab.arturbosch.detekt")
        plugin("org.jlleitschuh.gradle.ktlint")
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

    ktlint {
        debug.set(false)
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
        ignoreFailures.set(true)
        enableExperimentalRules.set(true)
        filter {
            exclude("**/generated/**")
            include("**/kotlin/**")
        }
    }

    configurations.all {
        if (!name.contains("detached")) {
            resolutionStrategy.activateDependencyLocking()
        }
    }

    detekt {
        config = rootProject.files("config/detekt/detekt.yml")
    }
}

rootProject.plugins.withType(NodeJsRootPlugin::class.java) {
    // 16+ required for Apple Silicon support
    // https://youtrack.jetbrains.com/issue/KT-49109#focus=Comments-27-5259190.0-0
    rootProject.the<NodeJsRootExtension>().download = true
    rootProject.the<NodeJsRootExtension>().nodeVersion = "18.11.0"
}

rootProject.plugins.withType(YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport = YarnLockMismatchReport.WARNING
    rootProject.the<YarnRootExtension>().reportNewYarnLock = false
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        html.outputLocation.set(file("build/reports/detekt.html"))
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.isNonStable()
    }
}

fun String.isNonStable() = "^[0-9,.v-]+(-r)?$".toRegex().matches(this).not()

tasks.register("reformatAll") {
    description = "Reformat all the Kotlin Code"

    dependsOn("ktlintFormat")
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:ktlintFormat"))
}

tasks.register("preMerge") {
    description = "Runs all the tests/verification tasks on both top level and included build."

    dependsOn("build", "test", "check")
    dependsOn("koverReport", "koverVerify", "koverMergedXmlReport")

    if ((properties["buildExamples"] as? String) == "true") {
        dependsOn(":example:fullstack:node:check")
        dependsOn(":example:fullstack:server:check")
    }
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:check"))
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:validatePlugins"))
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:koverReport"))
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:koverVerify"))
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register("resolveAndLockAll") {
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks)
    }
    dependsOn("resolveAllDependencies")
}

if (tasks.findByName("resolveAllDependencies") == null) {
    tasks.register("resolveAllDependencies") {
        val npmInstall = tasks.findByName("kotlinNpmInstall")
        if (npmInstall != null) {
            dependsOn(npmInstall)
        }
        doLast {
            allprojects {
                configurations.forEach { c ->
                    if (c.isCanBeResolved) {
                        println("Downloading dependencies for '$path' - ${c.name}")
                        val result = c.incoming.artifactView { lenient(true) }.artifacts
                        result.failures.forEach {
                            println("- Ignoring Error: ${it.message}")
                        }
                    }
                }
            }
        }
    }
}
