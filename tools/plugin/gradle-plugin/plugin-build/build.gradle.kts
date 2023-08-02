@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "DSL_SCOPE_VIOLATION",
)

import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("org.jetbrains.kotlin.kapt") apply false
    id("org.jetbrains.kotlinx.kover")
    id("io.gitlab.arturbosch.detekt")
    id("com.github.ben-manes.versions")
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.pluginPublish) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlinx.plugin.abiValidator)
}

val isCI = project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true"

apiValidation {
    nonPublicMarkers += listOf(
        "elide.annotations.Internal",
    )
}

koverMerged {
    enable()

    xmlReport {
        onCheck = isCI
    }

    htmlReport {
        onCheck = isCI
    }
}

allprojects {
    group = PluginCoordinates.GROUP
    version = PluginCoordinates.VERSION

    apply {
        plugin("io.gitlab.arturbosch.detekt")
        plugin("org.jlleitschuh.gradle.ktlint")
        plugin("org.jetbrains.kotlinx.kover")
    }

    ktlint {
        debug = false
        verbose = true
        android = false
        outputToConsole = true
        ignoreFailures = false
        enableExperimentalRules = true
        filter {
            exclude("**/generated/**")
            include("**/kotlin/**")
        }
    }

    detekt {
        config = rootProject.files("../config/detekt/detekt.yml")
    }
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required = true
        html.outputLocation = file("build/reports/detekt.html")
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.layout.buildDirectory)
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
