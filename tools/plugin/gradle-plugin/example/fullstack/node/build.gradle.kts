@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.bundler.BuildMode
import dev.elide.buildtools.bundler.js.BundleTarget
import dev.elide.buildtools.bundler.js.BundleTool
import tools.elide.assets.EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel

plugins {
    kotlin("js")
    alias(libs.plugins.node)
    id("dev.elide.buildtools.plugin")
}

elide {
    mode = BuildMode.DEVELOPMENT

    js {
        tool(BundleTool.ESBUILD)
        target(BundleTarget.EMBEDDED)

        runtime {
            inject(true)
            languageLevel(JsLanguageLevel.ES2020)
        }
    }
}
