@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode

plugins {
    kotlin("js")
    alias(libs.plugins.node)
    id("dev.elide.buildtools.plugin")
}

elide {
    mode.set(BuildMode.PRODUCTION)
}
