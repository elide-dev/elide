@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode
import tools.elide.assets.EmbeddedScriptLanguage

plugins {
  id("com.github.johnrengelman.shadow")
  id("io.micronaut.application")
  id("io.micronaut.aot")
  id("dev.elide.build.site.backend")
  id("dev.elide.build.docker")
  id("dev.elide.buildtools.plugin")
  id("dev.elide.build.native.app")
  id("com.google.devtools.ksp")
  alias(libs.plugins.jib)
}

group = "dev.elide.site.docs"
version = rootProject.version as String

elide {
  mode = if (devMode) {
    BuildMode.DEVELOPMENT
  } else {
    BuildMode.PRODUCTION
  }

  server {
    ssg {
      enable()
    }

    ssr(EmbeddedScriptLanguage.JS) {
      bundle(project(":site:docs:node"))
    }
    assets {
      bundler {
        format(tools.elide.assets.ManifestFormat.BINARY)
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
        from(project(":site:docs:ui"))
      }
    }
  }
}

micronaut {
  version.set(libs.versions.micronaut.lib.get())
  runtime.set(io.micronaut.gradle.MicronautRuntime.NETTY)
  processing {
    incremental.set(true)
    annotations.addAll(listOf(
      "$mainPackage.*",
    ))
  }
  aot {
    optimizeServiceLoading.set(true)
    convertYamlToJava.set(true)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    optimizeClassLoading.set(true)
    optimizeNetty.set(true)

    netty {
      enabled.set(true)
    }
  }
}

val mainPackage = "elide.docs"
val mainEntry = "$mainPackage.DocsApp"
val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"
val buildDocsSite: String by project.properties

application {
  mainClass.set(mainEntry)
  if (project.hasProperty("elide.vm.inspect") && project.properties["elide.vm.inspect"] == "true") {
    applicationDefaultJvmArgs = listOf(
      "-Delide.vm.inspect=true",
    )
  }
}

dependencies {
  api(libs.graalvm.sdk)
  ksp(project(":tools:processor"))
  api(project(":packages:base"))
  api(project(":packages:server"))
  api(project(":packages:graalvm"))

  implementation(libs.jsoup)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.runtime)
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.bouncycastle)
  implementation(libs.bouncycastle.pkix)
  implementation(libs.conscrypt)
  implementation(libs.tink)
  implementation(libs.netty.resolver.dns.native.macos)
  implementation(libs.netty.transport.native.unixCommon)
  implementation(libs.netty.transport.native.epoll)
  implementation(libs.netty.transport.native.kqueue)
  implementation(libs.netty.tcnative)
  implementation(libs.netty.tcnative.boringssl.static)
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-x86_64") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-aarch_64") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-x86_64") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-aarch_64") })
  runtimeOnly(libs.logback)

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(project(":packages:test"))
  testImplementation(libs.micronaut.test.junit5)
}

val shadowAppJar by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false
}

tasks.shadowJar {
  mergeServiceFiles()
  archiveClassifier.set("shadow")
}

artifacts {
  add("shadowAppJar", tasks.shadowJar)
}

tasks.named("apiCheck").configure {
  onlyIf { false }
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/jvm17")
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("optimizedDockerfile") {
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/jvm17")
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
  graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/builder:latest")
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/native:latest")
  args("-H:+StaticExecutableWithDynamicLibC")
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("optimizedDockerfileNative") {
  graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/builder:latest")
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/native:latest")
  args("-H:+StaticExecutableWithDynamicLibC")
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/site/docs/jvm:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/site/docs/jvm:opt-latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/site/docs/native:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/site/docs/native:opt-latest"
  ))
}

tasks {
  jib {
    from {
      image = "us-docker.pkg.dev/elide-fw/tools/runtime/jvm17:latest"
    }
    to {
      image = "us-docker.pkg.dev/elide-fw/samples/site/docs/jvm"
      tags = setOf("latest", "jib")
    }
    container {
      jvmFlags = listOf("-Ddyme.runtime=JVM", "-Xms512m", "-Xdebug")
      mainClass = mainEntry
      ports = listOf("8080", "50051")
      format = com.google.cloud.tools.jib.api.buildplan.ImageFormat.Docker
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
      quickBuild.set(false)
      buildArgs.addAll(listOf(
        "--no-fallback",
        "--language:js",
        "--language:regex",
        "--enable-http",
        "--enable-https",
        "--gc=G1",
        "--static",
        "--libc=glibc",
        "--enable-all-security-services",
        "--install-exit-handlers",
        "--report-unsupported-elements-at-runtime",
        "-Duser.country=US",
        "-Duser.language=en",
        "-H:IncludeLocales=en",
        "-H:+InstallExitHandlers",
        "-H:+ReportExceptionStackTraces",
        "-H:+DashboardAll",
        "-H:DashboardDump=/workspaces/v3/site/docs/app/analysis/dashboard.json",
        "-H:+DashboardJson",
        "--pgo-instrument",
        "-dsa",
        "--enable-all-security-services",
        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))
    }
  }
}
