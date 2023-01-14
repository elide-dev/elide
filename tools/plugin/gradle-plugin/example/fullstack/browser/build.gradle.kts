@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "DSL_SCOPE_VIOLATION",
)

plugins {
    kotlin("js")
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }
}

val assetDist by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(assetDist.name, tasks.named("browserDistribution").map { it.outputs.files.files.single() })
}
