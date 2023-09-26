@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  kotlin("js")
}

group = "dev.elide.site.docs"
version = rootProject.version as String

val kotlinWrapperVersion: String = libs.versions.kotlinxWrappers.get()
val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"

sourceSets {
  val mdx by creating {
    // holds MDX sources
  }
}

kotlin {
  js {
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

      testTask {
        useKarma {
          useSafari()
          useFirefox()
          useChrome()
        }
      }

      webpackTask {
        outputFileName = "ui.js"
        output.libraryTarget = "umd"
      }
    }
  }
}

dependencies {
  api(project(":site:docs:content"))
  api(project(":packages:base"))
  api(project(":packages:frontend"))
  api(project(":packages:graalvm-react"))

  implementation(devNpm("autoprefixer", "10.4.13"))
  implementation(devNpm("style-loader", "3.3.1"))
  implementation(devNpm("css-loader", "6.7.1"))
  implementation(devNpm("cssnano", "5.1.14"))
  implementation(devNpm("sass-loader", "13.2.0"))
  implementation(devNpm("sass", "1.56.1"))
  implementation(devNpm("postcss-loader", "7.0.2"))
  implementation(devNpm("postcss-preset-env", "7.8.3"))
  implementation(devNpm("postcss", "8.4.21"))
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
  implementation(libs.kotlinx.wrappers.js)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.kotlinx.wrappers.browser)
  implementation(libs.kotlinx.wrappers.history)
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

  add(assetDist.name, file(layout.buildDirectory.file("dist/js/productionExecutable/ui.js"))) {
    builtBy("browserDistribution")
  }
}

tasks.create("copyStaticSources", Copy::class.java) {
  from("$projectDir/src/main/assets") {
    include("**/*.*")
  }
  into("${rootProject.buildDir}/js/packages/elide-site-docs-ui/kotlin/")
}

tasks.create("copyMdxSources", Copy::class.java) {
  from("$projectDir/src/main/mdx") {
    include("**/*.*")
  }
  into("${rootProject.buildDir}/js/packages/elide-site-docs-ui/kotlin/")
}

tasks.named("browserDevelopmentRun").configure {
  dependsOn("copyMdxSources", "copyStaticSources")
}

tasks.named("browserProductionWebpack").configure {
  dependsOn("copyMdxSources", "copyStaticSources")
}

tasks.named("browserDistribution").configure {
  dependsOn("copyMdxSources", "copyStaticSources")
}

tasks.create("copyStaticAssets", Copy::class.java) {
  from("$projectDir/src/main/assets/") {
    include("*.*")
  }
  into(layout.buildDirectory.dir("processedResources/js/main/assets/"))
}

tasks.named("processResources").configure {
  dependsOn("copyStaticAssets")
}
