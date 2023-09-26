@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
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
  implementation(kotlin("stdlib"))
  implementation(libs.kotlinx.wrappers.js)
  implementation(projects.packages.base)
  implementation(project(":packages:graalvm-js"))
  implementation(npm("esbuild", libs.versions.npm.esbuild.get()))
}

tasks.withType<Tar> {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip>{
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
