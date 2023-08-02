@file:Suppress(
  "DSL_SCOPE_VIOLATION",
  "UnstableApiUsage",
)

plugins {
  `java-library`
  publishing
  jacoco

  kotlin("plugin.serialization")
  id("com.github.gmazzo.buildconfig")
  id("io.micronaut.application")
  id("io.micronaut.graalvm")
  id("io.micronaut.aot")
  id("dev.elide.build.jvm.kapt")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  explicitApi()
}

buildConfig {
  className("ElideSSGCompiler")
  packageName("elide.tool.ssg.cfg")
  useKotlinOutput()

  buildConfigField("String", "ELIDE_TOOL_VERSION", "\"${libs.versions.elide.asProvider().get()}\"")
}

val buildSamples: String by properties
val testProject = ":samples:server:hellocss"

val embeddedJars by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false
}

dependencies {
  api(libs.jakarta.inject)
  api(platform(libs.netty.bom))
  api(platform("io.micronaut:micronaut-bom:${libs.versions.micronaut.lib.get()}"))
  api(libs.slf4j)

  kapt(libs.micronaut.inject.java)
  kapt(libs.micronaut.validation)
  kapt(libs.picocli.codegen)
  kapt(libs.micronaut.serde.processor)

  implementation(libs.jackson.core)
  implementation(libs.jackson.databind)
  implementation(libs.jackson.jsr310)
  implementation(libs.jackson.module.kotlin)

  implementation(project(":packages:proto:proto-core"))
  implementation(project(":packages:proto:proto-protobuf"))
  implementation(project(":packages:proto:proto-kotlinx"))

  implementation(libs.guava)
  implementation(libs.commons.compress)
  implementation(platform(project(":packages:platform")))
  implementation(project(":packages:base"))
  implementation(project(":packages:server"))
  implementation(libs.jsoup)
  implementation(libs.picocli)
  implementation(kotlin("stdlib-jdk7"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.reactive)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.protobuf)

  implementation(libs.micronaut.inject.java)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.validation)
  implementation(libs.micronaut.picocli)
  implementation(libs.micronaut.http.client)
  implementation(libs.micronaut.graal)
  implementation(libs.micronaut.jackson.databind)
  implementation(libs.micronaut.kotlin.extension.functions)
  implementation(libs.micronaut.kotlin.runtime)

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

  implementation(libs.logback)
  implementation(libs.lz4)

  implementation(
    "io.netty:netty-resolver-dns-native-macos:4.1.95.Final:osx-aarch_64"
  )
  implementation(
    "io.netty:netty-resolver-dns-native-macos:4.1.95.Final:osx-x86_64"
  )

  runtimeOnly(libs.micronaut.runtime)
  runtimeOnly(libs.micronaut.runtime.osx)
  runtimeOnly(libs.brotli)
  runtimeOnly(libs.brotli.native.osx.amd64)
  runtimeOnly(libs.brotli.native.osx.arm64)
  runtimeOnly(libs.brotli.native.linux.amd64)

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testImplementation(libs.micronaut.test.junit5)

  if (buildSamples == "true") {
    testImplementation(project(testProject))

    embeddedJars(project(
      testProject,
      configuration = "shadowAppJar",
    ))
  }
}

application {
  mainClass = "elide.tool.ssg.SiteCompiler"
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
    }
  }
}

sonarqube {
  isSkipProject = true
}

micronaut {
  version = libs.versions.micronaut.lib.get()
  runtime = io.micronaut.gradle.MicronautRuntime.NETTY
  processing {
    incremental = true
    annotations.addAll(listOf(
      "elide.tool.ssg.*",
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

tasks.test {
  if (buildSamples != "true") {
    enabled = false
  }

  useJUnitPlatform()
  systemProperty("elide.test", "true")
  systemProperty("tests.buildDir", layout.buildDirectory.dir("ssgTests"))
  systemProperty("tests.exampleManifest", layout.buildDirectory.file("resources/test/app.manifest.pb"))
  systemProperty("tests.textManifest", layout.buildDirectory.file("resources/test/example-manifest.txt.pb"))
  systemProperty("tests.invalidManifest", layout.buildDirectory.file("resources/test/example-invalid.txt.pb"))
}

tasks.compileTestKotlin {
  if (buildSamples != "true") {
    enabled = false
  }
}

tasks {
  named<JavaExec>("run") {
    systemProperty("micronaut.environments", "dev")
  }
}

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)

afterEvaluate {
  tasks.named("testNativeImage") {
    enabled = false
  }
}

graalvmNative {
  testSupport = false  // disabled for now due to micronaut test bugs in native

  metadataRepository {
    enabled = true
    version = GraalVMVersions.graalvmMetadata
  }

  binaries {
    named("main") {
      fallback = false
      sharedLibrary = false
      buildArgs.addAll(listOf(
        "--language:regex",
        "--gc=epsilon",
        "--libc=glibc",
        "--enable-http",
        "--enable-https",
        "--no-fallback",
        "--install-exit-handlers",
        "--initialize-at-build-time=org.slf4j.LoggerFactory",
        "--initialize-at-build-time=org.slf4j.simple.SimpleLogger",
        "--initialize-at-build-time=org.slf4j.impl.StaticLoggerBinder",
        "--initialize-at-run-time=io.netty.util.internal.logging.Log4JLogger",
        "--initialize-at-run-time=io.netty.util.AbstractReferenceCounted",
        "--initialize-at-run-time=io.netty.channel.epoll",
        "--initialize-at-run-time=io.netty.handler.ssl",
        "--initialize-at-run-time=io.netty.channel.unix",
        "-Duser.country=US",
        "-Duser.language=en",
        "-H:IncludeLocales=en",
        "--enable-all-security-services",
      ))
      quickBuild = quickbuild
    }

    named("test") {
      buildArgs.addAll(listOf(
        "--language:regex",
        "--enable-all-security-services",
      ))
      quickBuild = quickbuild
    }
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
