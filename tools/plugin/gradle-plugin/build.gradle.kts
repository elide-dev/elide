@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import java.util.Properties

plugins {
    java
    alias(libs.plugins.kotlin.kapt) apply false
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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val props = Properties()
val overlay = file(if (project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true") {
    "gradle-ci.properties"
} else {
    "local.properties"
})

if (overlay.exists()) props.load(overlay.inputStream())

sonarqube {
    properties {
        property("sonar.projectKey", "elide-dev_buildtools")
        property("sonar.organization", "elide-dev")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.dynamicAnalysis", "reuseReports")
        property("sonar.junit.reportsPath", "build/reports/")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.jacoco.reportPath", "build/jacoco/test.exec")
        property("sonar.sourceEncoding", "UTF-8")
    }
}

subprojects {
    apply {
        plugin("io.gitlab.arturbosch.detekt")
        plugin("org.jlleitschuh.gradle.ktlint")
        plugin("org.sonarqube")
    }

    sonarqube {
        properties {
            property("sonar.sources", "src/main/java")
            property("sonar.tests", "src/test/java")
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                listOf(
                    "build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml",
                    "build/reports/jacoco/testCodeCoverageReport/jacocoTestReport.xml",
                    "build/reports/jacoco/test/jacocoTestReport.xml",
                    "build/reports/kover/xml/coverage.xml",
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

    detekt {
        config = rootProject.files("config/detekt/detekt.yml")
    }
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

    dependsOn(":example:fullstack:node:check")
    dependsOn(":example:fullstack:server:check")
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:check"))
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:validatePlugins"))
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
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
