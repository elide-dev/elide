plugins {
  kotlin("jvm")
  kotlin("plugin.atomicfu")
  alias(libs.plugins.micronaut.minimal.library)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.elide.conventions)
}

private fun <T> onlyIf(flag: Boolean, value: T): T? = value.takeIf { flag }
private fun <T> List<T>.onlyIf(flag: Boolean): List<T> = if (flag) this else emptyList()

/** Whether to enable Panama-based tests for the shared native binary. */
val nativeTest = findProperty("elide.embedded.tests.native")?.toString()?.toBooleanStrictOrNull() == true

val oracleGvm = false
val jvmTarget = 22
val stamp = (project.properties["elide.stamp"] as? String ?: "false").toBooleanStrictOrNull() ?: false
val cliVersion = if (stamp) {
  libs.versions.elide.asProvider().get()
} else {
  "1.0-dev-${System.currentTimeMillis() / 1000 / 60 / 60 / 24}"
}

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)
val isRelease = !quickbuild && (
  project.properties["elide.release"] == "true" ||
  project.properties["elide.buildMode"] == "release"
)
val isDebug = !isRelease && (
  project.properties["elide.buildMode"] == "debug"
)

val nativesType = if (isRelease) "release" else "debug"
val targetPath = rootProject.layout.projectDirectory.dir("target/$nativesType")

val rootPath: String = rootProject.layout.projectDirectory.asFile.path
val thirdPartyPath: String = rootProject.layout.projectDirectory.dir("third_party").asFile.path
val sqliteLibPath: String = rootProject.layout.projectDirectory.dir("third_party/sqlite/install/lib").asFile.path

val nativeImageBuildDebug = properties["nativeImageBuildDebug"] == "true"
val nativeImageBuildVerbose = properties["nativeImageBuildVerbose"] == "true"

val nativesRootTemplate: (String) -> String = { version ->
  "/tmp/elide-runtime/v$version/native"
}

val nativesPath = nativesRootTemplate(cliVersion)
version = cliVersion

val jvmCompileArgs = listOfNotNull(
  "--enable-preview",
  "--add-modules=jdk.incubator.vector",
  "--enable-native-access=" + listOfNotNull("ALL-UNNAMED").joinToString(","),
)

val ktCompilerArgs = listOf(
  "-jvm-default=no-compatibility",
  "-Xallow-unstable-dependencies",
  "-Xcontext-receivers",
  "-Xemit-jvm-type-annotations",
  "-Xlambdas=indy",
  "-Xsam-conversions=indy",
  "-Xjsr305=strict",
  "-Xjavac-arguments=${jvmCompileArgs.joinToString(",")}}",

  // opt-in to Elide's delicate runtime API
  "-opt-in=elide.runtime.core.DelicateElideApi",
)

val sharedLibArgs = sequenceOf(
  "-march=compatibility",
  "--enable-http",
  "--enable-https",
  "--enable-all-security-services",
  "--color=always",
  "-H:CStandard=C11",
  "-H:+ReportExceptionStackTraces",
  "-H:+UnlockExperimentalVMOptions",
  "-H:-RemoveUnusedSymbols",
  "-Delide.natives=$nativesPath",
  "-J-Delide.natives=$nativesPath",
  "-J-Dtruffle.TrustAllTruffleRuntimeProviders=true",
  "-J-Dpolyglot.image-build-time.PreinitializeContextsWithNative=true",
  "-J-Dpolyglot.image-build-time.PreinitializeContexts=" + listOfNotNull(
    "js",
  ).joinToString(","),
)

val releaseArgs = sequenceOf(
  "-O2",
)

val debugArgs = sequenceOf(
  "-g",
  "-H:+SourceLevelDebug",
)

val initializeAtRunTime = sequenceOf(
  "elide.runtime.intrinsics.server.http.netty.NettyRequestHandler",
  "elide.runtime.intrinsics.server.http.netty.NettyHttpResponse",
  "elide.runtime.intrinsics.server.http.netty.NettyTransport",
  "elide.runtime.intrinsics.server.http.netty.KQueueTransport",
  "elide.runtime.intrinsics.server.http.netty.EpollTransport",
  "elide.runtime.intrinsics.server.http.netty.IOUringTransport",
  "elide.runtime.intrinsics.server.http.netty.NettyTransport",
  "elide.runtime.intrinsics.server.http.netty.NettyTransport${'$'}Companion",
  "elide.runtime.gvm.internals.node.process.NodeProcess${'$'}NodeProcessModuleImpl",
  "io.netty.buffer.AbstractReferenceCountedByteBuf",
  "io.netty.buffer.PooledByteBufAllocator",
  "io.netty.buffer.ByteBufAllocator",
  "io.netty.buffer.ByteBufUtil",
  "io.netty.buffer.AbstractReferenceCountedByteBuf",
  "io.netty.resolver.dns.DefaultDnsServerAddressStreamProvider",
  "io.netty.resolver.dns.DnsServerAddressStreamProviders${'$'}DefaultProviderHolder",
  "io.netty.resolver.dns.DnsNameResolver",
  "io.netty.resolver.HostsFileEntriesResolver",
  "io.netty.resolver.dns.ResolvConf${'$'}ResolvConfLazy",
  "io.netty.resolver.dns.DefaultDnsServerAddressStreamProvider",
  "io.netty.handler.codec.http2.Http2CodecUtil",
  "io.netty.handler.codec.http2.Http2ClientUpgradeCodec",
  "io.netty.handler.codec.http2.Http2ConnectionHandler",
  "io.netty.handler.codec.http2.DefaultHttp2FrameWriter",
  "io.netty.incubator.channel.uring",
  "io.netty.incubator.channel.uring.IOUringEventLoopGroup",
  "io.netty.incubator.channel.uring.Native",
  "io.netty.handler.codec.http.HttpObjectEncoder",
  "io.netty.internal.tcnative.SSL",
  "io.micronaut.core.util.KotlinUtils",
  "io.micronaut.core.io.socket.SocketUtils",
  "io.micronaut.core.type.RuntimeTypeInformation${'$'}LazyTypeInfo",
  "io.micronaut.context.env.CachedEnvironment",
  "io.micronaut.context.env.exp.RandomPropertyExpressionResolver",
  "io.micronaut.context.env.exp.RandomPropertyExpressionResolver${'$'}LazyInit",
  "com.sun.jna.platform.mac.CoreFoundation",
  "com.sun.jna.Structure${'$'}FFIType",
  "com.sun.jna.platform.mac.IOKit",
  "com.sun.jna.platform.mac.IOKitUtil",
  "com.sun.jna.platform.mac.SystemB",
  "oshi.hardware.platform.linux",
  "oshi.hardware.platform.mac",
  "oshi.hardware.platform.mac.MacFirmware",
  "oshi.hardware.platform.unix",
  "oshi.hardware.platform.unix.aix",
  "oshi.hardware.platform.unix.freebsd",
  "oshi.hardware.platform.unix.openbsd",
  "oshi.hardware.platform.unix.solaris",
  "oshi.hardware.platform.windows",
  "oshi.jna.platform.mac.IOKit",
  "oshi.jna.platform.mac.SystemB",
  "oshi.jna.platform.mac.SystemConfiguration",
  "oshi.software.os",
  "oshi.software.os.linux",
  "oshi.software.os.linux.LinuxOperatingSystem",
  "oshi.software.os.mac",
  "oshi.software.os.mac.MacOperatingSystem",
  "oshi.software.os.unix.aix",
  "oshi.software.os.unix.aix.AixOperatingSystem",
  "oshi.software.os.unix.freebsd",
  "oshi.software.os.unix.freebsd.FreeBsdOperatingSystem",
  "oshi.software.os.unix.openbsd",
  "oshi.software.os.unix.openbsd.OpenBsdOperatingSystem",
  "oshi.software.os.unix.solaris",
  "oshi.software.os.unix.solaris.SolarisOperatingSystem",
  "oshi.software.os.windows",
  "oshi.software.os.windows.WindowsOperatingSystem",
  "oshi.util.platform.linux.DevPath",
  "oshi.util.platform.linux.ProcPath",
  "oshi.util.platform.linux.SysPath",
  "oshi.util.platform.mac.CFUtil",
  "oshi.util.platform.mac.SmcUtil",
  "oshi.util.platform.mac.SysctlUtil",
  "oshi.util.platform.unix.freebsd.BsdSysctlUtil",
  "oshi.util.platform.unix.freebsd.ProcstatUtil",
  "oshi.util.platform.unix.openbsd.FstatUtil",
  "oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil",
  "oshi.util.platform.unix.solaris.KstatUtil",
)

fun nativeImageArgs(release: Boolean = isRelease, debug: Boolean = isDebug): List<String> {
  return sharedLibArgs.plus(listOf(
    "--initialize-at-build-time=",
  ).plus(initializeAtRunTime.joinToString(",").let {
    if (it.isNotEmpty()) "--initialize-at-run-time=$it" else ""
  })).plus(
    when {
      release -> releaseArgs
      debug -> debugArgs
      else -> sequenceOf()
    }
  ).plus(
    listOf("--debug-attach").onlyIf(nativeImageBuildDebug)
  ).plus(
    listOf(
      targetPath,
      sqliteLibPath,
    ).plus(
      System.getProperty("java.library.path", "")
        .split(File.pathSeparator)
        .filter { it.isNotEmpty() }
    ).distinct().joinToString(
      File.pathSeparator
    ).let {
      listOf(
        "-Djava.library.path=$it",
      )
    }
  ).toList()
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(jvmTarget))
    if (oracleGvm) {
      vendor.set(JvmVendorSpec.matching("Oracle Corporation"))
    } else {
      vendor.set(JvmVendorSpec.GRAAL_VM)
    }
  }
}

elide {
  kotlin {
    explicitApi = true
    powerAssert = false
    customKotlinCompilerArgs = ktCompilerArgs.toMutableList()
  }

  checks {
    spotless = false
  }
}

kotlin {
  explicitApi()

  // suppress delicate usage warnings for Elide core runtime APIs
  // and opt-in for the new context receivers syntax
  compilerOptions.optIn.add("elide.runtime.core.DelicateElideApi")
  compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")

  jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(jvmTarget))
  }
}

dependencies {
  // elide
  implementation(projects.packages.base)
  implementation(projects.packages.engine)
  implementation(projects.packages.graalvm)

  // protobuf
  api(libs.protobuf.java)
  api(libs.protobuf.util)
  api(libs.protobuf.kotlin)

  // language engines
  implementation(libs.graalvm.js.language)
  implementation(libs.graalvm.wasm.language)
  implementation(projects.packages.graalvmJs)
  implementation(projects.packages.graalvmTs)
  implementation(projects.packages.graalvmPy)
  implementation(projects.packages.graalvmRb)

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

tasks.detekt {
  enabled = false
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
    quickBuild = quickbuild
    fallback = false
    buildArgs.addAll(nativeImageArgs())
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

