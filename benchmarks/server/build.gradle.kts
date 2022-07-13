@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode
import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.allopen.gradle.*
import tools.elide.assets.EmbeddedScriptLanguage

plugins {
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")
  id("dev.elide.buildtools.plugin")
  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)
}

elide {
  mode = BuildMode.PRODUCTION

  server {
    ssr(EmbeddedScriptLanguage.JS) {
      bundle(project(":samples:fullstack:react-ssr:node"))
    }
    assets {
      bundler {
        format(tools.elide.assets.ManifestFormat.BINARY)
        compression {
          minimumSizeBytes(0)
          keepAllVariants()
        }
      }

      script("scripts.ui") {
        from(project(":samples:fullstack:react-ssr:frontend"))
      }
    }
  }
}

sourceSets.all {
  java.setSrcDirs(listOf("jmh/src"))
  resources.setSrcDirs(listOf("jmh/resources"))
}

dependencies {
  kapt(libs.micronaut.inject.java)
  implementation(libs.kotlinx.benchmark.runtime)
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)
  implementation(libs.micronaut.aop)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject.java.test)
  implementation(libs.micronaut.http.client)
  implementation(libs.micronaut.http.server)
  implementation(libs.micronaut.http.validation)
  implementation(libs.micronaut.validation)
  implementation(libs.micronaut.runtime)
  implementation(libs.micronaut.router)
  implementation(libs.kotlinx.wrappers.css)
  runtimeOnly(libs.logback)
  implementation(project(":packages:server"))
}

configure<AllOpenExtension> {
  annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
  configurations {
    named("main") {
      warmups = 3
      iterations = 3
      include( if (project.hasProperty("elide.benchmark")) {
        project.properties["elide.benchmark"] as String
      } else {
        "elide.benchmarks.*"
      })
      exclude(
        "elide.benchmarks.PageBenchmarkHttp"
      )
    }
  }
  targets {
    register("main") {
      this as JvmBenchmarkTarget
      jmhVersion = "1.21"
    }
  }
}
