@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import com.google.cloud.tools.jib.api.buildplan.ImageFormat.Docker
import dev.elide.buildtools.gradle.plugin.BuildMode.DEVELOPMENT
import dev.elide.buildtools.gradle.plugin.BuildMode.PRODUCTION
import io.micronaut.gradle.MicronautRuntime.NETTY
import tools.elide.assets.EmbeddedScriptLanguage
import tools.elide.assets.ManifestFormat

plugins {
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization") version "2.0.20-Beta1"
  id("dev.elide.buildtools.plugin")
  id("io.micronaut.application")
  id("io.micronaut.docker")
  id("io.micronaut.graalvm")
  id("io.micronaut.aot")
  alias(libs.plugins.jib)
}

group = "dev.elide.samples"
version = rootProject.version as String
val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"

java {
  sourceCompatibility = JavaVersion.VERSION_22
  targetCompatibility = JavaVersion.VERSION_22
}

kotlin {
  jvmToolchain(22)
}

elideApp {
  injectDependencies = false

  mode = if (devMode) {
    DEVELOPMENT
  } else {
    PRODUCTION
  }

  server {
    ssr(EmbeddedScriptLanguage.JS) {
      bundle(projects.fullstack.reactSsr.node)
    }
    assets {
      bundler {
        format(ManifestFormat.BINARY)

        compression {
          minimumSizeBytes(0)
          keepAllVariants()
        }
      }

      // stylesheet: `styles.base`
      stylesheet("styles.base") {
        sourceFile("src/main/assets/basestyles.css")
      }

      script("scripts.ui") {
        from(projects.fullstack.reactSsr.frontend)
      }
    }
  }
}

micronaut {
  version = libs.versions.micronaut.lib.get()
  runtime = NETTY

  processing {
    incremental = true
    annotations.addAll(listOf(
      "$mainPackage.*",
    ))
  }

  aot {
    optimizeServiceLoading = true
    convertYamlToJava = true
    precomputeOperations = true
    cacheEnvironment = true
    optimizeClassLoading = true
    optimizeNetty = true

    netty {
      enabled = true
    }
  }
}

val mainPackage = "fullstack.reactssr"
val mainEntry = "$mainPackage.App"

application {
  mainClass = mainEntry

  if (project.hasProperty("elide.vm.inspect") && properties["elide.vm.inspect"] == "true") {
    applicationDefaultJvmArgs = listOf(
      "-Delide.vm.inspect=true",
    )
  }
}

dependencies {
  api(kotlin("stdlib"))
  api(kotlin("stdlib-jdk8"))
  kapt(mn.micronaut.inject.java)
  implementation(framework.elide.base)
  implementation(framework.elide.ssr)
  implementation(framework.elide.server)
  implementation(framework.elide.graalvm)

  implementation(libs.jsoup)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.runtime)
  implementation(mn.micronaut.reactor)
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.bouncycastle)
  implementation(libs.bouncycastle.pkix)
  implementation(libs.conscrypt)
  implementation(libs.tink)
  runtimeOnly(libs.logback)
  runtimeOnly(mn.snakeyaml)

  testImplementation(kotlin("test-junit5"))
  testImplementation(framework.elide.test)
  testImplementation(mn.micronaut.test.junit5)
}

tasks {
  dockerBuild {
    images = listOf(
      "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react-ssr/jvm:latest"
    )
  }

  optimizedDockerBuild {
    images = listOf(
      "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react-ssr/jvm:opt-latest"
    )
  }

  dockerBuildNative {
    images = listOf(
      "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react-ssr/native:latest"
    )
  }

  optimizedDockerBuildNative {
    images = listOf(
      "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react-ssr/native:opt-latest"
    )
  }

  jib {
    from {
      image = "us-docker.pkg.dev/elide-fw/tools/runtime/jvm17:latest"
    }
    to {
      image = "us-docker.pkg.dev/elide-fw/samples/fullstack/react-ssr/jvm"
      tags = setOf("latest", "jib")
    }
    container {
      jvmFlags = listOf("-Delide.runtime=JVM", "-Xms512m", "-Xdebug")
      mainClass = mainEntry
      ports = listOf("8080", "50051")
      format = Docker
    }
  }
}

graalvmNative {
  testSupport = false

  metadataRepository {
    enabled = true
    version = "0.3.3"
  }

  agent {
    defaultMode = "standard"
    builtinCallerFilter = true
    builtinHeuristicFilter = true
    enableExperimentalPredefinedClasses = false
    enableExperimentalUnsafeAllocationTracing = false
    trackReflectionMetadata = true
    enabled = System.getenv("GRAALVM_AGENT") == "true"

    modes {
      standard {}
    }
    metadataCopy {
      inputTaskNames.add("test")
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting = true
    }
  }

  binaries {
    named("main") {
      fallback = false
      quickBuild = false

      buildArgs.addAll(listOf(
        "-g",
        "--no-fallback",
        "--enable-http",
        "--enable-https",
        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))
    }
  }
}

tasks {
  distTar {
    enabled = false
  }
  distZip {
    enabled = false
  }
}

afterEvaluate {
  listOf(
    "buildLayers",
    "optimizedBuildLayers",
  ).forEach {
    tasks.named(it).configure {
      doNotTrackState("too big for build cache")
    }
  }
}
