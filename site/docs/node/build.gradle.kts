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
  id("dev.elide.build.site.frontend")
  id("dev.elide.buildtools.plugin")
}

group = "dev.elide.site.docs"
version = rootProject.version as String

val devMode = (project.property("elide.buildMode") ?: "dev") != "prod"

elide {
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
  api(npm("@emotion/css", "11.10.5"))
  api(npm("@emotion/react", "11.10.5"))
  api(npm("@emotion/cache", "11.10.5"))
  api(npm("@emotion/server", "11.10.0"))

  api(project(":packages:ssr"))
  api(project(":site:docs:content"))
  implementation(project(":packages:base"))
  implementation(project(":packages:graalvm-js"))
  implementation(project(":packages:graalvm-react"))
  implementation(project(":site:docs:ui"))

  // Kotlin Wrappers
  implementation(libs.kotlinx.wrappers.react)
  implementation(libs.kotlinx.wrappers.react.dom)
  implementation(libs.kotlinx.wrappers.react.router.dom)
  implementation(libs.kotlinx.wrappers.remix.run.router)
  implementation(libs.kotlinx.wrappers.emotion)
}
