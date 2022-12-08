@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.site.frontend")
}

group = "dev.elide.site.docs"
version = rootProject.version as String

val kotlinWrapperVersion = libs.versions.kotlinxWrappers.get()
val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"

kotlin {
  js(IR) {
    binaries.executable()
    browser {
      commonWebpackConfig {
        sourceMaps = true
        cssSupport {
          enabled = true
        }
        mode = if (devMode) {
          org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.DEVELOPMENT
        } else {
          org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.PRODUCTION
        }
      }

      testTask {
        useKarma {
          useSafari()
          useFirefox()
          useChrome()
        }
      }

      webpackTask {
        outputFileName = "site.js"
        output.libraryTarget = "umd"
      }
    }
  }
}

dependencies {
  api(project(":site:docs:content"))
  api(project(":site:docs:ui"))
  implementation(project(":packages:base"))
  implementation(project(":packages:frontend"))
  implementation(project(":packages:graalvm-react"))

  implementation(devNpm("style-loader", "3.3.1"))
  implementation(devNpm("css-loader", "6.7.1"))
  implementation(devNpm("sass-loader", "13.2.0"))
  implementation(devNpm("sass", "1.56.1"))
  implementation(npm("react", libs.versions.react.get()))
  implementation(npm("react-dom", libs.versions.react.get()))

  implementation(kotlin("stdlib-js"))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.kotlinx.wrappers.browser)
  implementation(libs.kotlinx.wrappers.react)
  implementation(libs.kotlinx.wrappers.react.dom)
  implementation(libs.kotlinx.wrappers.react.router.dom)
  implementation(libs.kotlinx.wrappers.remix.run.router)
  implementation(libs.kotlinx.wrappers.emotion)
  implementation(libs.kotlinx.wrappers.mui)
  implementation(libs.kotlinx.wrappers.mui.icons)
  implementation(libs.kotlinx.wrappers.styled)
}

val assetDist by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false
}

artifacts {
  add(assetDist.name, file("${buildDir}/distributions/site.js")) {
    builtBy("browserDistribution")
  }
}
