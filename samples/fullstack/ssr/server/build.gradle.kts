@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.samples.backend")
  id("dev.elide.build.native.app")
  id("dev.elide.buildtools.plugin")
  id("io.micronaut.application")
  id("io.micronaut.aot")
}

group = "dev.elide.samples"
version = rootProject.version as String
val devMode = (project.property("elide.buildMode") ?: "dev") != "prod"

application {
  mainClass.set("fullstack.ssr.App")
}

elide {
  mode = if (devMode) {
    dev.elide.buildtools.gradle.plugin.BuildMode.DEVELOPMENT
  } else {
    dev.elide.buildtools.gradle.plugin.BuildMode.PRODUCTION
  }

  server {
    ssr(tools.elide.assets.EmbeddedScriptLanguage.JS) {
      bundle(project(":samples:fullstack:ssr:node"))
    }
  }
}

micronaut {
  version.set(libs.versions.micronaut.lib.get())
  runtime.set(io.micronaut.gradle.MicronautRuntime.NETTY)
  processing {
    incremental.set(true)
    annotations.add("fullstack.ssr.*")
  }
  aot {
    optimizeServiceLoading.set(true)
    convertYamlToJava.set(true)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    netty {
      enabled.set(true)
    }
  }
}

graalvmNative {
  testSupport.set(false)

  metadataRepository {
    enabled.set(true)
    version.set(GraalVMVersions.graalvmMetadata)
  }

  agent {
    defaultMode.set("standard")
    builtinCallerFilter.set(true)
    builtinHeuristicFilter.set(true)
    enableExperimentalPredefinedClasses.set(false)
    enableExperimentalUnsafeAllocationTracing.set(false)
    trackReflectionMetadata.set(true)
    enabled.set(true)

    modes {
      standard {}
    }
    metadataCopy {
      inputTaskNames.add("test")
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting.set(true)
    }
  }

  binaries {
    named("main") {
      fallback.set(false)
      quickBuild.set(true)
      buildArgs.addAll(listOf(
        "--no-fallback",
        "--language:js",
//        "--language:regex",
//        "--enable-http",
//        "--enable-https",
//        "--gc=G1",
//        "--static",
//        "--libc=glibc",
//        "--enable-all-security-services",
//        "--install-exit-handlers",
//        "--report-unsupported-elements-at-runtime",
//        "-Duser.country=US",
//        "-Duser.language=en",
//        "-H:IncludeLocales=en",
//        "-H:+InstallExitHandlers",
//        "-H:+ReportExceptionStackTraces",
//        "--pgo-instrument",
//        "-dsa",
//        "--language:js",
//        "--language:regex",
        "--enable-all-security-services",
//        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))
    }
  }
}

dependencies {
  implementation(projects.packages.base)
  implementation(project(":packages:server"))
  implementation(project(":packages:graalvm"))
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.runtime)
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.wrappers.css)
  runtimeOnly(libs.logback)
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/ssr/jvm:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/ssr/jvm:opt-latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/ssr/native:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/ssr/native:opt-latest"
  ))
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

