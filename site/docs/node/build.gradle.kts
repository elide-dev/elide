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
val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"

elide {
  mode = if (devMode) {
    BuildMode.DEVELOPMENT
  } else {
    BuildMode.PRODUCTION
  }

  js {
    prepack(false)
    minify(true)
    target(BundleTarget.EMBEDDED)
    esbuild(file("${projectDir}/esbuild-site-ssr.mjs.hbs"))

    runtime {
      inject(true)
      languageLevel(JsLanguageLevel.ES2022)
    }
  }
}

kotlin {
  js(IR) {
    browser {
      binaries.executable()

      commonWebpackConfig {
        sourceMaps = devMode
        cssSupport {
          enabled.set(false)  // we are handling this manually
        }
        mode = if (devMode) {
          org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.DEVELOPMENT
        } else {
          org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.PRODUCTION
        }
      }

      webpackTask {
        output.libraryTarget = "umd"
      }
    }
  }
}

dependencies {
  api(npm("@emotion/css", "11.10.5"))
  api(npm("@emotion/react", "11.10.5"))
  api(npm("@emotion/cache", "11.10.5"))
  api(npm("@emotion/server", "11.10.0"))

  implementation(devNpm("autoprefixer", "10.4.13"))
  implementation(devNpm("style-loader", "3.3.1"))
  implementation(devNpm("css-loader", "6.7.1"))
  implementation(devNpm("cssnano", "5.1.14"))
  implementation(devNpm("sass-loader", "13.2.0"))
  implementation(devNpm("sass", "1.56.1"))
  implementation(devNpm("postcss-loader", "7.0.2"))
  implementation(devNpm("postcss-preset-env", "7.8.3"))
  implementation(devNpm("postcss", "8.4.21"))
  implementation(devNpm("@mdx-js/esbuild", "2.2.1"))
  implementation(devNpm("@mdx-js/loader", "2.2.1"))
  implementation(npm("@mdx-js/react", "2.2.1"))
  implementation(npm("@mdx-js/mdx", "2.2.1"))
  implementation(npm("react-syntax-highlighter", "15.5.0"))

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

tasks.create("copyMdxSources", Copy::class.java) {
  from("${project(":site:docs:ui").projectDir}/src/main/mdx") {
    include("**/*.*")
  }
  into("${rootProject.buildDir}/js/packages/elide-node/kotlin/")
}

tasks.create("copyMdxSourcesSsr", Copy::class.java) {
  from("${project(":site:docs:ui").projectDir}/src/main/mdx") {
    include("**/*.*")
  }
  into("${layout.buildDirectory}/ssr/")
}

listOf(
  "browserDevelopmentWebpack",
  "browserProductionWebpack",
).forEach {
  tasks.named(it).configure {
    enabled = false
  }
}

listOf(
  "generateDevelopmentEsBuildConfig",
  "generateProductionEsBuildConfig",
).forEach {
  tasks.named(it).configure {
    dependsOn("copyMdxSourcesSsr")
  }
}
