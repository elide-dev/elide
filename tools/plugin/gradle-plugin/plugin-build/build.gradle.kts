/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
    "UnstableApiUsage",
    "unused",
    "DSL_SCOPE_VIOLATION",
)

import io.gitlab.arturbosch.detekt.Detekt

plugins {
    kotlin("kapt") apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    alias(libs.plugins.versionCheck)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.pluginPublish) apply false
    alias(libs.plugins.kotlinx.plugin.abiValidator)
}

val isCI = project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true"

apiValidation {
    nonPublicMarkers += listOf(
        "elide.annotations.Internal",
    )
}

koverReport {
    defaults {
        xml {
            onCheck = isCI
        }
    }
}

allprojects {
    group = PluginCoordinates.GROUP
    version = PluginCoordinates.VERSION

    apply {
        plugin("io.gitlab.arturbosch.detekt")
        plugin("org.jetbrains.kotlinx.kover")
    }

    detekt {
        config.from(rootProject.files("../config/detekt/detekt.yml"))
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
