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
val devMode = true

sourceSets {
  val mdx by creating {
    // it holds MDX sources
  }
}

kotlin {
  js(IR) {
    browser {
      binaries.executable()

      commonWebpackConfig {
        sourceMaps = devMode
        cssSupport {
          enabled.set(true)
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
        outputFileName = "ui.js"
        output.libraryTarget = "module"
      }
    }
  }
}

dependencies {
  api(project(":site:docs:content"))
  api(project(":packages:base"))
  api(project(":packages:frontend"))
  api(project(":packages:graalvm-react"))

  implementation(devNpm("style-loader", "3.3.1"))
  implementation(devNpm("css-loader", "6.7.1"))
  implementation(devNpm("sass-loader", "13.2.0"))
  implementation(devNpm("sass", "1.56.1"))
  implementation(devNpm("@mdx-js/esbuild", "2.2.1"))
  implementation(devNpm("@mdx-js/loader", "2.2.1"))
  implementation(npm("@mdx-js/react", "2.2.1"))
  implementation(npm("@mdx-js/mdx", "2.2.1"))
  implementation(npm("react-syntax-highlighter", "15.5.0"))

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
val assetStatic by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false
}

artifacts {
  add(assetStatic.name, file("${projectDir}/src/main/assets/base.css"))

  add(assetDist.name, file("${buildDir}/distributions/ui.js")) {
    builtBy("browserDistribution")
  }
}

tasks.create("copyMdxSources", Copy::class.java) {
  from("$projectDir/src/main/mdx") {
    include("**/*.*")
  }
  into("${rootProject.buildDir}/js/packages/elide-ui/kotlin/")
}

tasks.named("browserDevelopmentRun").configure {
  dependsOn("copyMdxSources")
}

tasks.named("browserDistribution").configure {
  dependsOn("copyMdxSources")
}

tasks.create("copyStaticAssets", Copy::class.java) {
  from("$projectDir/src/main/assets/") {
    include("*.*")
  }
  into("${buildDir}/processedResources/js/main/assets/")
}

tasks.named("processResources").configure {
  dependsOn("copyStaticAssets")
}
