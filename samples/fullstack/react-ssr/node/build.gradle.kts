@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode
import dev.elide.buildtools.gradle.plugin.js.BundleTarget
import dev.elide.buildtools.gradle.plugin.js.BundleTool
import tools.elide.assets.EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel

plugins {
  kotlin("js")
  id("dev.elide.buildtools.plugin")
}

group = "dev.elide.samples"
version = rootProject.version as String

val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"

kotlin {
  js {
    browser()
    generateTypeScriptDefinitions()

    compilations.all {
      kotlinOptions {
        sourceMap = true
        moduleKind = "umd"
        metaInfo = true
      }
    }
  }
}

elideApp {
  mode = if (devMode) {
    BuildMode.DEVELOPMENT
  } else {
    BuildMode.PRODUCTION
  }

  js {
    tool(BundleTool.ESBUILD)
    target(BundleTarget.EMBEDDED)

    runtime {
      inject(true)
      languageLevel(JsLanguageLevel.ES2020)
    }
  }
}

dependencies {
  implementation(framework.elide.base)
  implementation(framework.elide.graalvm.js)
  implementation(framework.elide.graalvm.react)
  implementation(projects.fullstack.reactSsr.frontend)

  // Kotlin Wrappers
  implementation(libs.kotlinx.wrappers.react)
  implementation(libs.kotlinx.wrappers.react.dom)
}
