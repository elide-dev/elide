plugins {
  kotlin("jvm")
  kotlin("plugin.atomicfu")
  alias(libs.plugins.micronaut.library)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.buildConfig)

  id(libs.plugins.ksp.get().pluginId)
  id("elide.internal.conventions")
}

/** Whether to enable Panama-based tests for the shared native binary. */
val nativeTest = findProperty("elide.embedded.tests.native")?.toString()?.toBooleanStrictOrNull() == true

kotlin {
  explicitApi()

  // suppress delicate usage warnings for Elide core runtime APIs
  // and opt-in for the new context receivers syntax
  compilerOptions.optIn.add("elide.runtime.core.DelicateElideApi")
  compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
}

dependencies {
  // elide
  implementation(projects.packages.base)
  implementation(projects.packages.graalvm)
  implementation(projects.packages.proto.protoProtobuf)

  // micronaut
  implementation(mn.micronaut.core)

  // kotlinx
  implementation(libs.kotlinx.atomicfu)

  // graalvm
  compileOnly(libs.graalvm.svm)

  runtimeOnly(libs.logback)

  testImplementation(kotlin("test"))
  testImplementation(libs.kotlinx.coroutines.test)
}

buildConfig {
  sourceSets.named("main") {
    packageName = "elide.embedded.interop"
    className = "BuildConstants"

    // set the path to the typedef header to be used during the native build
    // note that this value is not used at runtime, so it's safe to embed it
    buildConfigField<String>(
      name = "TYPEDEF_HEADER_PATH",
      value = layout.projectDirectory.file("src/main/native/elide_embedded_types.h").asFile.absolutePath,
    )
  }

  sourceSets.named("test") {
    packageName = "elide.embedded.test"
    className = "TestConstants"

    // set the path to the shared library built by the 'nativeCompile' task
    // this value is used during tests to load the binary for use with Panama
    buildConfigField<String>(
      name = "ELIDE_EMBEDDED_PATH",
      value = layout.buildDirectory.file("native/nativeCompile/libelide.so").map { it.asFile.absolutePath },
    )
  }
}

graalvmNative {
  binaries.named("main") {
    imageName = "libelide"
    sharedLibrary = true

    quickBuild = true
    fallback = false

    val initializeAtBuildTime = sequenceOf(
      // SLF4J + Logback (used by static loggers)
      "ch.qos.logback",
      "org.slf4j.LoggerFactory",
      // referenced by `elide.runtime.feature.js.JavaScriptFeature`
      "elide.runtime.gvm.internals.GraalVMGuest",
      // required by `io.micronaut.core.util.KotlinUtils`
      "kotlin.coroutines.intrinsics.CoroutineSingletons"
    ).map { "--initialize-at-build-time=$it" }

    buildArgs.addAll(initializeAtBuildTime.toList())
  }
}

tasks.test {
  useJUnitPlatform()

  // when the native library tests are enabled, use a fresh binary
  if (nativeTest) dependsOn(tasks.nativeCompile)

  // toggle C interop (Panama-based) tests for the shared library
  systemProperties["elide.embedded.tests.interop"] = "$nativeTest"
  systemProperties["kotlinx.coroutines.test.default_timeout"] = "5s"

  // suppress Panama-related warnings when using downcalls
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}
