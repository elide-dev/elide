/*
 * Copyright (c) 2023-2024 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress("UnstableApiUsage")

import io.micronaut.gradle.MicronautRuntime
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom
import org.jetbrains.kotlin.konan.target.HostManager
import kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension

import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  kotlin("plugin.atomicfu")
  kotlin("plugin.allopen")
  kotlin("plugin.noarg")

  alias(libs.plugins.micronaut.application)
  alias(libs.plugins.micronaut.aot)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.shadow)
  alias(libs.plugins.kover)
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.gradle.checksum)

  id(libs.plugins.ksp.get().pluginId)
  id("elide.internal.conventions")
}

group = "dev.elide.embedded"
version = rootProject.version as String

// Flags affecting this build script:
//
// - `elide.release`: true/false
// - `elide.buildMode`: `dev`, `release`, etc
// - `elide.targetOs`: `darwin`, `linux`, `windows`
// - `elide.targetArch`: `amd64`, `arm64`

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)
val isRelease = !quickbuild && (
  project.properties["elide.release"] == "true" ||
  project.properties["elide.buildMode"] == "release"
)

val oracleGvm = true
val enableEdge = true
val enableWasm = true
val enablePython = true
val enableRuby = true
val enableTools = true
val enableMosaic = true
val enableProguard = false
val enableLlvm = true
val enableEspresso = true
val enableExperimental = false
val enableEmbeddedResources = true
val enableResourceFilter = true
val enableAuxCache = false
val enableJpms = false
val enableEmbeddedBuilder = false
val enableDashboard = false
val enableBuildReport = false
val enableStrictHeap = false
val enableG1 = oracleGvm && HostManager.hostIsLinux
val enablePgo = false
val enablePgoSampling = false
val enablePgoInstrumentation = false
val enableSbom = true
val enableSbomStrict = false
val enableTruffleJson = enableEdge
val encloseSdk = !System.getProperty("java.vm.version").contains("jvmci")
val globalExclusions = emptyList<Pair<String, String>>()

val entrypoint = "elide.embedded.Entrypoint"
val module = "elide.embedded"

val moduleExclusions = listOf(
  "io.micronaut" to "micronaut-core-processor",
)

val nativeHeaderPath: String = layout.projectDirectory.file("src/native/elide.h").asFile.path

val jvmCompileArgs = listOfNotNull(
  "--enable-preview",
  "--add-modules=jdk.incubator.vector",
  "-Delide.embedded.headerPath=$nativeHeaderPath",
  "--enable-native-access=" + listOfNotNull(
    "ALL-UNNAMED",
    "org.graalvm.polyglot",
    "org.graalvm.js",
    if (enableRuby) "org.graalvm.ruby" else null,
    if (enablePython) "org.graalvm.py" else null,
    if (enableEspresso) "org.graalvm.espresso" else null,
  ).joinToString(","),
  "--add-exports=org.graalvm.truffle/com.oracle.truffle.object=ALL-UNNAMED",
  "--add-exports=org.graalvm.truffle.runtime/com.oracle.truffle.runtime=ALL-UNNAMED",
  "--add-exports=jdk.internal.vm.compiler/org.graalvm.compiler.options=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.c=ALL-UNNAMED",
  "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED",
).plus(if (enableJpms) listOf(
  "--add-reads=elide.embedded=ALL-UNNAMED",
  "--add-reads=elide.graalvm=ALL-UNNAMED",
  "--add-reads=elide.protocol.protobuf=ALL-UNNAMED",
  "--add-exports=java.base/jdk.internal.module=elide.embedded",
  "--add-exports=jdk.internal.vm.compiler/org.graalvm.compiler.options=elide.embedded",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.option=elide.embedded",
) else emptyList()).plus(if (enableEmbeddedBuilder) listOf(
  "--add-exports=org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.option=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jni=ALL-UNNAMED",
) else emptyList())

val jvmRuntimeArgs = emptyList<String>()

val nativeCompileJvmArgs = jvmCompileArgs.map {
  "-J$it"
}

val jvmModuleArgs = listOf(
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
).plus(jvmCompileArgs).plus(jvmRuntimeArgs)

val ktCompilerArgs = listOf(
  "-Xallow-unstable-dependencies",
  "-Xcontext-receivers",
  "-Xemit-jvm-type-annotations",
  "-Xlambdas=indy",
  "-Xsam-conversions=indy",
  "-Xjsr305=strict",
  "-Xjvm-default=all",
  "-Xextended-compiler-checks",
  "-Xjavac-arguments=${jvmCompileArgs.joinToString(",")}}",

  // opt-in to Elide's delicate runtime API
  "-opt-in=elide.runtime.core.DelicateElideApi",
)

val targetOs = when {
  Os.isFamily(Os.FAMILY_WINDOWS) -> "windows"
  Os.isFamily(Os.FAMILY_MAC) -> "darwin"
  Os.isFamily(Os.FAMILY_UNIX) -> "linux"
  else -> "generic"
}

val stamp = (project.properties["elide.stamp"] as? String ?: "false").toBooleanStrictOrNull() ?: false
val embeddedVersion: String = if (stamp) {
  libs.versions.elide.asProvider().get()
} else {
  "1.0-dev-${System.currentTimeMillis() / 1000 / 60 / 60 / 24}"
}

val headers: Configuration by configurations.creating
val jpmsModules: Configuration by configurations.creating
configurations.compileClasspath.extendsFrom(configurations.named("jpmsModules"))

// @TODO(sgammon): proper modulepath
val modules = configurations.api

/**
 * Build: Embedded Native Image (Shared Library)
 */

val commonGvmArgs = listOf(
  "-H:+UseCompressedReferences",
).plus(if (enableBuildReport) listOf("-H:+BuildReport") else emptyList())

val commonNativeArgs = listOfNotNull(
  "--no-fallback",
  "--enable-preview",
  "--enable-http",
  "--enable-https",
  "--enable-all-security-services",
  "--no-install-exit-handlers",
  "--configure-reflection-metadata",
  "-H:CStandard=C11",
  "-H:DefaultCharset=UTF-8",
  "-H:+UseContainerSupport",
  "-H:+ReportExceptionStackTraces",
  "-H:+AddAllCharsets",
  "-H:TempDirectory=${layout.buildDirectory.dir("native/tmp").get().asFile.path}",
  "--trace-class-initialization=io.netty.channel.DefaultFileRegion,io.netty.util.AbstractReferenceCounted",
  if (enableEspresso) "-H:+AllowJRTFileSystem" else null,
  if (enableEspresso) "-J-Djdk.image.use.jvm.map=false" else null,
  if (enableEspresso) "-J-Despresso.finalization.UnsafeOverride=true" else null,
  "-R:MaxDirectMemorySize=256M",
  "-J-Dio.netty.allocator.type=unpooled",
  "-J-Dpolyglot.image-build-time.PreinitializeContextsWithNative=true",
  "-J-Dpolyglot.image-build-time.PreinitializeContexts=" + listOfNotNull(
    "js",
    if (enableExperimental && enableRuby) "ruby" else null,
    if (enableExperimental && enablePython) "python" else null,
    if (enableExperimental && enableEspresso) "java" else null,
  ).joinToString(","),
  if (enablePgoInstrumentation) "--pgo-instrument" else null,
  if (enablePgoSampling) "--pgo-sampling" else null,
).asSequence().plus(if (enableEdge) listOf(
  "-H:+UnlockExperimentalVMOptions",
) else emptyList()).plus(
  if (oracleGvm) commonGvmArgs else emptyList()
).toList()

val dashboardFlags: List<String> = listOf(
  "-H:DashboardDump=elide-embedded",
  "-H:+DashboardAll",
)

val debugFlags: List<String> = listOfNotNull(
  "-march=compatibility",
).plus(
  if (enableDashboard) dashboardFlags else emptyList(),
).plus(
  if (HostManager.hostIsLinux) listOf(
    "-g",
  ) else emptyList(),
)

val experimentalFlags = listOf(
  "-H:+SupportContinuations",  // -H:+SupportContinuations is in use, but is not supported together with Truffle JIT compilation
  "-H:+UseStringInlining",  // String inlining optimization is not supported when just-in-time compilation is used

  // Not enabled for regular builds yet
  "-H:±UseExperimentalReachabilityAnalysis",
  "-H:±UseNewExperimentalClassInitialization",
  "-H:±UseDedicatedVMOperationThread",
  "-H:±SupportCompileInIsolates",

  "-R:±AlwaysInlineIntrinsics",
  "-R:±AlwaysInlineVTableStubs",

  // Profiling
  "-R:±ProfileMonitors",
  "-R:±ProfileOptBulkAllocation",
  "-R:±ProfileCompiledMethods",
  "-R:±ProfileConstantObjects",
  "-R:±ProfileLockElimination",

  // Crashes
  "-H:+ProtectionKeys",
  "-H:+UseThinLocking",

  // Significant slowdowns
  "-H:+RunMainInNewThread",
)

// CFlags for release mode.
val releaseCFlags: List<String> = listOf(
  "-O3",
  "-v",
).plus(
  if (!enableRuby) listOf(
    "-flto",
  ) else emptyList(),
)

// PGO profiles to specify in release mode.
val profiles: List<String> = listOf(
  "embedded.iprof",
)

// GVM release flags
val gvmReleaseFlags: List<String> = listOf(
  "-H:+AOTInline",
  "-H:+VectorizeSIMD",
  "-H:+LSRAOptimization",
  "-H:+MLProfileInference",
  "-H:+BouncyCastleIntrinsics",
  "-R:+BouncyCastleIntrinsics",
  "-H:+VectorPolynomialIntrinsics",
)

// Full release flags (for all operating systems and platforms).
val releaseFlags: List<String> = listOf(
  "-O3",
  "-H:+LocalizationOptimizedMode",
  "-H:+RemoveUnusedSymbols",
).asSequence().plus(releaseCFlags.flatMap {
  listOf(
    "-H:NativeLinkerOption=$it",
    "--native-compiler-options=$it",
  )
}).plus(if (enablePgo) listOf(
  "--pgo=${profiles.joinToString(",")}",
  "-H:CodeSectionLayoutOptimization=ClusterByEdges",
) else emptyList()).plus(listOf(
  if (enableSbom) listOf(
    if (enableSbomStrict) "--enable-sbom=cyclonedx,export,strict" else "--enable-sbom=cyclonedx,export"
  ) else emptyList(),
  if (enableDashboard) dashboardFlags else emptyList(),
  if (oracleGvm) gvmReleaseFlags else emptyList(),
).flatten()).toList()

val jvmDefs = mapOf(
  "user.country" to "US",
  "user.language" to "en",
  "io.netty.allocator.type" to "unpooled",
  "logback.statusListenerClass" to "ch.qos.logback.core.status.NopStatusListener",
  "slf4j.provider" to "ch.qos.logback.classic.spi.LogbackServiceProvider",
)

val hostedRuntimeOptions = mapOf(
  "IncludeLocales" to "en",
)

val initializeAtBuildTime = listOf(
  // Elide Embedded
  "elide.embedded.BeanConfigurationFactory",
  "elide.embedded.BeanDefinitionReferenceFactory",
  "elide.embedded.BeanIntrospectionReferenceFactory",
  "elide.embedded.HttpRequestFactoryFactory",
  "elide.embedded.HttpResponseFactoryFactory",
  "elide.embedded.TypeConverterRegistrarFactory",
  "elide.embedded.PropertySourceLoaderFactory",
  "elide.embedded.PropertyExpressionResolverFactory",

  // Kotlin Core
  "kotlin._Assertions",
  "kotlin.KotlinVersion",
  "kotlin.DeprecationLevel",
  "kotlin.annotation.AnnotationRetention",
  "kotlin.annotation.AnnotationTarget",
  "kotlin.SafePublicationLazyImpl",
  "kotlin.LazyThreadSafetyMode",
  "kotlin.LazyKt__LazyJVMKt${'$'}WhenMappings",

  // Kotlin Standard Library
  "kotlin.coroutines.ContinuationInterceptor",
  "kotlin.sequences",
  "kotlin.text.Charsets",
  "kotlin.text.Regex",
  "kotlin.time",
  "kotlin.time.DurationJvmKt",
  "kotlin.time.Duration",
  "kotlin.time.DurationUnit",

  // Kotlin Reflect / JVM Internals
  "kotlin.jvm.internal.CallableReference",
  "kotlin.jvm.internal.Reflection",
  "kotlin.jvm.internal.PropertyReference",
  "kotlin.jvm.internal.PropertyReference1",
  "kotlin.jvm.internal.PropertyReference1Impl",
  "kotlin.reflect.jvm.internal.CachesKt",
  "kotlin.reflect.jvm.internal.CacheByClassKt",
  "kotlin.reflect.jvm.internal.KClassImpl",
  "kotlin.reflect.jvm.internal.KClassImpl${'$'}Data",
  "kotlin.reflect.jvm.internal.KProperty1Impl",
  "kotlin.reflect.jvm.internal.KDeclarationContainerImpl",
  "kotlin.reflect.jvm.internal.KDeclarationContainerImpl${'$'}Data",
  "kotlin.reflect.jvm.internal.RuntimeTypeMapper",
  "kotlin.reflect.jvm.internal.impl.builtins.CompanionObjectMapping",
  "kotlin.reflect.jvm.internal.impl.builtins.PrimitiveType",
  "kotlin.reflect.jvm.internal.impl.builtins.StandardNames",
  "kotlin.reflect.jvm.internal.impl.builtins.StandardNames${'$'}FqNames",
  "kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind${'$'}Function",
  "kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind${'$'}KFunction",
  "kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind${'$'}SuspendFunction",
  "kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind${'$'}KSuspendFunction",
  "kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap",
  "kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap${'$'}PlatformMutabilityMapping",
  "kotlin.reflect.jvm.internal.impl.descriptors.runtime.structure.ReflectClassUtilKt",
  "kotlin.reflect.jvm.internal.impl.name.FqName",
  "kotlin.reflect.jvm.internal.impl.name.FqNameUnsafe",
  "kotlin.reflect.jvm.internal.impl.name.SpecialNames",
  "kotlin.reflect.jvm.internal.impl.name.StandardClassIds",
  "kotlin.reflect.jvm.internal.impl.name.StandardClassIdsKt",
  "kotlin.reflect.jvm.internal.impl.resolve.jvm.JvmPrimitiveType",

  // KotlinX Modules
  "kotlinx.datetime",
  "kotlinx.io",
  "kotlinx.coroutines",
  "kotlinx.serialization",
  "kotlinx.serialization.internal",

  // KotlinX Serialization + KotlinX JSON
  "kotlinx.serialization.internal.StringSerializer",
  "kotlinx.serialization.modules.SerializersModuleKt",
  "kotlinx.serialization.json.Json",
  "kotlinx.serialization.json.Json${'$'}Default",
  "kotlinx.serialization.json.JsonElementKt",
  "kotlinx.serialization.json.internal.CharArrayPoolBatchSize",
  "kotlinx.serialization.json.internal.StreamingJsonDecoder${'$'}WhenMappings",
  "kotlinx.serialization.json.internal.WriteMode",
  "kotlinx.serialization.json.internal.ArrayPoolsKt",
  "kotlinx.serialization.json.internal.ByteArrayPool8k",

  // Google Commons + Protobuf
  "com.google.protobuf",
  "com.google.common.html.types.Html",
  "com.google.common.jimfs.Feature",
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
  "com.google.common.collect.MapMakerInternalMap",
  "com.google.common.collect.MapMakerInternalMap${'$'}StrongKeyWeakValueSegment",
  "com.google.common.collect.MapMakerInternalMap${'$'}EntrySet",
  "com.google.common.collect.MapMakerInternalMap${'$'}StrongKeyWeakValueEntry${'$'}Helper",
  "com.google.common.collect.MapMakerInternalMap${'$'}1",
  "com.google.common.base.Equivalence${'$'}Equals",

  // SLF4J + Logback
  "ch.qos.logback",
  "org.slf4j.MarkerFactory",
  "org.slf4j.simple.SimpleLogger",
  "org.slf4j.impl.StaticLoggerBinder",
  "org.slf4j.LoggerFactory",
  "org.slf4j.helpers.Reporter",
  "org.slf4j.helpers.SubstituteServiceProvider",
  "org.slf4j.helpers.SubstituteLoggerFactory",
  "org.slf4j.helpers.Reporter${'$'}TargetChoice",
  "org.slf4j.helpers.Reporter${'$'}Level",
  "org.slf4j.helpers.NOPLoggerFactory",
  "org.slf4j.helpers.NOP_FallbackServiceProvider",

  // Encodings, Parsers, Cryptography
  "com.sun.tools.doclint",
  "org.codehaus.stax2.typed.Base64Variants",
  "org.bouncycastle.util.Properties",
  "org.bouncycastle.util.Strings",
  "org.bouncycastle.crypto.macs.HMac",
  "org.bouncycastle.crypto.prng.drbg.Utils",
  "org.bouncycastle.jcajce.provider.drbg.DRBG",
  "org.bouncycastle.jcajce.provider.drbg.EntropyDaemon",
  "org.xml.sax.helpers.LocatorImpl",
  "org.xml.sax.helpers.AttributesImpl",
  "jdk.jshell.Snippet${'$'}SubKind",
  "com.sun.tools.javac.parser.Tokens${'$'}TokenKind",

  // Databasing
  "org.sqlite.util.ProcessRunner",

  // Micronaut
  "io.micronaut.context.env.ConstantPropertySources",
  "io.micronaut.core.async.ReactiveStreamsTypeInformationProvider",
  "io.micronaut.core.reflect.ClassUtils${'$'}Optimizations",
  "io.micronaut.core.async.publisher.PublishersOptimizations",
  "io.micronaut.inject.provider.ProviderTypeInformationProvider",
  "io.micronaut.inject.beans.visitor.MapperAnnotationMapper",
  "io.micronaut.inject.beans.visitor.JsonCreatorAnnotationMapper",
  "io.micronaut.inject.beans.visitor.IntrospectedToBeanPropertiesTransformer",
  "io.micronaut.inject.beans.visitor.persistence.JakartaMappedSuperClassIntrospectionMapper",
  "io.micronaut.http.cookie.SameSiteConverter",
  "io.micronaut.http.util.HttpTypeInformationProvider",

  // --- Elide ------

  // Elide Runtime Core
  "elide.runtime.core.HostPlatform${'$'}Architecture",
  "elide.runtime.core.HostPlatform${'$'}OperatingSystem",
  "elide.runtime.plugins.env.EnvConfig${'$'}EnvVariableSource",

  // Elide VM Internals
  "tools.elide",
  "elide.runtime.gvm",
  "elide.runtime.gvm.internals",
  "elide.runtime.gvm.internals.context",
  "elide.runtime.gvm.internals.intrinsics",
  "elide.runtime.gvm.internals.vfs",
  "elide.runtime.gvm.internals.AbstractGVMScript",
  "elide.runtime.gvm.internals.AbstractVMAdapter",
  "elide.runtime.gvm.internals.IntrinsicsManager",
  "elide.runtime.gvm.internals.VMConditionalMultiProperty",
  "elide.runtime.gvm.internals.VMConditionalProperty",
  "elide.runtime.gvm.internals.VMRuntimeProperty",
  "elide.runtime.gvm.internals.VMStaticProperty",
  "elide.runtime.gvm.internals.GraalVMGuest",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}JVM",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}JAVASCRIPT",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}PYTHON",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}RUBY",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}WASM",

  // Elide VM Intrinsics: JavaScript
  "elide.runtime.intrinsics",
  "elide.runtime.intrinsics.js",
  "elide.runtime.intrinsics.js.err",
  "elide.runtime.intrinsics.js.express",
  "elide.runtime.intrinsics.js.typed",
  "elide.runtime.gvm.js",
  "elide.runtime.gvm.internals.intrinsics.js",
  "elide.runtime.gvm.internals.intrinsics.js.base64",
  "elide.runtime.gvm.internals.intrinsics.js.console",
  "elide.runtime.gvm.internals.intrinsics.js.crypto",
  "elide.runtime.gvm.internals.intrinsics.js.express",
  "elide.runtime.gvm.internals.intrinsics.js.fetch",
  "elide.runtime.gvm.internals.intrinsics.js.stream",
  "elide.runtime.gvm.internals.intrinsics.js.struct",
  "elide.runtime.gvm.internals.intrinsics.js.typed",
  "elide.runtime.gvm.internals.intrinsics.js.url",
  "elide.runtime.gvm.internals.intrinsics.js.webstreams",

  // Elide Packages: Framework
  "elide.annotations",
  "elide.base",
  "elide.core",
  "elide.core.crypto",
  "elide.core.encoding",
  "elide.core.encoding.base64",
  "elide.core.encoding.hex",
  "elide.core.platform",
  "elide.util",
  "elide.runtime.jvm",

  // Elide Internals: Engine Implementations
  "elide.runtime.gvm.internals.js",
  "elide.runtime.gvm.internals.python",
  "elide.runtime.gvm.internals.ruby",
  "elide.runtime.gvm.internals.jvm",
  "elide.runtime.gvm.internals.wasm",

  // Elide Internals: VM Engines
  "elide.runtime.gvm.internals.AbstractVMEngine",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo${'$'}NativeImageInfo",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeNativeResources${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeNativeResourceBundle${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo${'$'}NativeImageInfo${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeNativeResourceSignature${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeVFS${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo${'$'}UniversalImageInfo",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo${'$'}UniversalImageInfo${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeArtifact${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeInfo",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeInfo${'$'}${'$'}serializer",
)

val initializeAtBuildTimeTest: List<String> = listOf(
  "org.junit.platform.launcher.core.LauncherConfig",
  "org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter",
  "org.slf4j.helpers.Reporter${'$'}TargetChoice",
  "org.slf4j.helpers.Reporter${'$'}Level",
  "org.slf4j.helpers.NOP_FallbackServiceProvider",
)

val initializeAtRuntime: List<String> = listOf(
  "com.sun.tools.javac.file.Locations",
  "ch.qos.logback.core.AsyncAppenderBase${'$'}Worker",
  "io.micronaut.core.util.KotlinUtils",
  "io.micrometer.common.util.internal.logging.Slf4JLoggerFactory",
  "io.netty.handler.codec.compression.BrotliOptions",
  "io.netty.handler.codec.http.cookie.ServerCookieEncoder",
  "io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator",
  "io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder",
  "org.truffleruby.aot.ParserCache",
  "org.truffleruby.core.encoding.Encodings",
  "org.truffleruby.core.format.FormatEncoding",
  "org.truffleruby.core.format.rbsprintf.RBSprintfSimpleTreeBuilder",
  "org.truffleruby.core.format.printf.PrintfSimpleTreeBuilder",
  "org.truffleruby.core.string.FrozenStrings",
  "org.truffleruby.parser.lexer.RubyLexer${'$'}Keyword${'$'}Maps",
  "kotlin.random.AbstractPlatformRandom",
  "kotlin.random.XorWowRandom",
  "kotlin.random.Random${'$'}Default",
  "kotlin.random.RandomKt",
  "kotlin.random.jdk8.PlatformThreadLocalRandom",
  "kotlin.internal.jdk8.JDK8PlatformImplementations",
  "kotlin.internal.jdk8.JDK8PlatformImplementations${'$'}ReflectSdkVersion",
  "kotlin.jvm.internal.MutablePropertyReference1Impl",
  "io.netty.channel.kqueue.KQueue",
  "io.netty.channel.kqueue.Native",
  "io.netty.channel.epoll.EPoll",
  "io.netty.channel.epoll.Native",
)

val initializeAtRuntimeTest: List<String> = emptyList()

val rerunAtRuntime: List<String> = listOf(
  "org.bouncycastle.jcajce.provider.drbg.DRBG$${'$'}Default",
)

val rerunAtRuntimeTest: List<String> = emptyList()

val defaultPlatformArgs = listOf(
  "--libc=glibc",
)

val windowsOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=native",
  "--gc=serial",
  "-Delide.vm.engine.preinitialize=true",
  "-H:InitialCollectionPolicy=Adaptive",
  "-R:MaximumHeapSizePercent=80",
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else emptyList())).plus(if (oracleGvm && enableAuxCache) listOf(
  "-H:-AuxiliaryEngineCache",
) else emptyList())

val darwinOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=native",
  "--gc=serial",
  "-Delide.vm.engine.preinitialize=true",
  "-H:InitialCollectionPolicy=Adaptive",
  "-R:MaximumHeapSizePercent=80",
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else listOf(
  "-J-Xmx24g",
))).plus(if (oracleGvm && enableAuxCache) listOf(
  "-H:+AuxiliaryEngineCache",
) else emptyList())

val windowsReleaseArgs = windowsOnlyArgs

val darwinReleaseArgs = darwinOnlyArgs.plus(
  listOf(
    "-march=native",
  ),
)

val linuxOnlyArgs = defaultPlatformArgs.plus(
  listOf(
    "--static",
    "-march=native",
    "-H:RuntimeCheckedCPUFeatures=" + listOf(
      "AVX",
      "AVX2",
      "AMD_3DNOW_PREFETCH",
      "SSE3",
      "LZCNT",
      "TSCINV_BIT",
      "ERMS",
      "CLMUL",
      "SHA",
      "VZEROUPPER",
      "FLUSH",
      "FLUSHOPT",
      "HV",
      "FSRM",
      "CET_SS",
    ).joinToString(","),
    "-H:+StaticExecutableWithDynamicLibC",
  ),
).plus(
  if (enableG1) listOf(
    "--gc=G1",
    "-H:+UseG1GC",
    "-H:-AuxiliaryEngineCache",
    "-Delide.vm.engine.preinitialize=false",
  ) else listOf(
    "--gc=serial",
    "-Delide.vm.engine.preinitialize=true",
    "-R:MaximumHeapSizePercent=80",
    "-H:InitialCollectionPolicy=Adaptive",
  ).plus(if (oracleGvm && enableAuxCache) listOf(
    "-H:+AuxiliaryEngineCache",
  ) else emptyList())
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else listOf(
  "-J-Xmx24g",
))

val linuxGvmReleaseFlags = listOf(
  "-H:+ObjectInlining",
)

val linuxReleaseArgs = linuxOnlyArgs.plus(
  listOf(
    "-R:+WriteableCodeCache",
    "-H:+StripDebugInfo",
  ).plus(if (oracleGvm) linuxGvmReleaseFlags else emptyList()),
)

val muslArgs = listOf(
  "--libc=musl",
)

val testOnlyArgs: List<String> = emptyList()

val isEnterprise: Boolean = properties["elide.graalvm.variant"] == "ENTERPRISE"

// ====================================================================================================================
// ====================================================================================================================

application {
  mainClass = entrypoint
  applicationDefaultJvmArgs = jvmRuntimeArgs
  if (enableJpms) mainModule = module
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
  if (enableJpms) modularity.inferModulePath = true
}

sourceSets {
  val main by getting {
    if (enableJpms) java.srcDirs(
      layout.projectDirectory.dir("src/main/java9"),
    )
  }
  val native by creating
}

testlogger {
  theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
  showExceptions = System.getenv("TEST_EXCEPTIONS") == "true"
  showFailed = true
  showPassed = true
  showSkipped = true
  showFailedStandardStreams = true
  showStandardStreams = System.getenv("TEST_STDOUT") == "true"
  showPassedStandardStreams = System.getenv("TEST_STDOUT") == "true"
  showFullStackTraces = true
  slowThreshold = 30000L
}

elide {
  publishing {
    id = "server"
    name = "Elide for Embedded"
    description = "Embedded host interfaces for Elide applications."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
    kotlinVersionOverride = "1.9"  // no support in KSP yet for v2.0
  }

  java {
    includeJavadoc = false
    includeSources = false
    configureModularity = enableJpms
    moduleName = module
  }

  docker {
    useGoogleCredentials = true
  }

  jvm {
    alignVersions = false
  }
}

apply(plugin = "kotlinx-atomicfu")

the<AtomicFUPluginExtension>().apply {
  dependenciesVersion = libs.versions.atomicfu.get()
  transformJvm = true
  transformJs = true
  jvmVariant = "VH"
}

buildConfig {
  className("ElideEmbedded")
  packageName("elide.embedded.api.cfg")
  useKotlinOutput()

  buildConfigField("String", "ELIDE_RELEASE_TYPE", if (isRelease) "\"RELEASE\"" else "\"DEV\"")
  buildConfigField("String", "ELIDE_TOOL_VERSION", "\"$embeddedVersion\"")
}

micronaut {
  version = libs.versions.micronaut.lib.get()
  runtime = MicronautRuntime.NETTY
  enableNativeImage(true)

  processing {
    incremental = true
    annotations.addAll(listOf(
      "elide.embedded",
      "elide.embedded.*",
      "elide.embedded.annotations",
      "elide.embedded.annotations.*",
    ))
  }

  aot {
    configFile = file("$projectDir/aot-native.properties")
    this@aot.version = libs.versions.micronaut.aot.get()

    convertYamlToJava = true
    precomputeOperations = true
    cacheEnvironment = true
    deduceEnvironment = true
    replaceLogbackXml = true

    optimizeServiceLoading = true
    optimizeClassLoading = true
    optimizeNetty = true
    possibleEnvironments = listOf("embedded", "cloud")

    netty {
      enabled = true
      machineId = "13-37-7C-D1-6F-F5"
      pid = "1337"
    }
  }
}

dependencies {
  // Dependencies: Baseline
  annotationProcessor(mn.micronaut.inject.java)
  ksp(mn.micronaut.inject.kotlin)
  ksp(libs.protobuf.java)
  modules(kotlin("stdlib"))

  // Dependencies: Project
  headers(projects.packages.nfi)
  modules(projects.packages.core)
  modules(projects.packages.base)
  modules(projects.packages.http)
  modules(projects.packages.serverless)
  modules(projects.packages.graalvm)
  modules(projects.packages.graalvmRb)
  modules(projects.packages.graalvmPy)
  modules(projects.packages.graalvmKt)
  modules(projects.packages.graalvmJvm)
  modules(projects.packages.graalvmJava)
  modules(projects.packages.graalvmWasm)
  modules(projects.packages.proto.protoCore)
  modules(projects.packages.proto.protoKotlinx)
  modules(projects.packages.proto.protoProtobuf)
  modules(projects.packages.runtime)

  // Dependencies: KotlinX
  modules(libs.kotlinx.atomicfu)
  modules(libs.kotlinx.coroutines.core)
  modules(libs.kotlinx.coroutines.jdk8)
  modules(libs.kotlinx.coroutines.jdk9)
  modules(libs.kotlinx.coroutines.reactive)
  modules(libs.kotlinx.coroutines.guava)
  modules(libs.kotlinx.collections.immutable)
  modules(libs.kotlinx.datetime)
  modules(libs.kotlinx.io)
  modules(libs.kotlinx.html)
  modules(libs.kotlinx.serialization.json)
  modules(libs.kotlinx.serialization.protobuf)

  // Dependencies: Micronaut
  api(mn.micronaut.core)
  api(mn.micronaut.core.reactive)
  api(mn.micronaut.http)
  api(mn.micronaut.http.netty)

  // Dependencies: Google
  modules(libs.protobuf.java)
  modules(libs.protobuf.util)
  modules(libs.protobuf.kotlin)
  modules(libs.grpc.core)
  modules(libs.grpc.api)
  modules(libs.grpc.stub)
  modules(libs.grpc.kotlin.stub)
  modules(libs.grpc.inprocess)
  modules(libs.grpc.protobuf)
  modules(libs.grpc.services)
  modules(libs.grpc.netty)

  // Dependencies: Micronaut AOT
  aotOptimizerRuntimeClasspath(mn.logback.core)
  aotOptimizerRuntimeClasspath(mn.logback.classic)

  // Dependencies: GraalVM
  modules(libs.graalvm.polyglot)
  modules(libs.graalvm.polyglot.tools.coverage)
  modules(libs.graalvm.polyglot.tools.dap)
  modules(libs.graalvm.polyglot.tools.inspect)
  modules(libs.graalvm.polyglot.tools.insight)
  modules(libs.graalvm.polyglot.tools.heap)
  modules(libs.graalvm.polyglot.tools.profiler)
  modules(libs.graalvm.regex)

  // GraalVM: JavaScript + WASM
  modules(libs.graalvm.polyglot.js)
  modules(libs.graalvm.polyglot.wasm)
  modules(libs.graalvm.js.language)
  modules(libs.graalvm.js.isolate)

  // GraalVM: Python
  modules(libs.graalvm.polyglot.python)
  modules(libs.graalvm.python.language)
  modules(libs.graalvm.python.resources)

  // GraalVM: Ruby
  modules(libs.graalvm.polyglot.ruby)
  modules(libs.graalvm.ruby.language)
  modules(libs.graalvm.ruby.resources)

  // GraalVM: Espresso
  modules(libs.graalvm.polyglot.java)
  modules(libs.graalvm.espresso.polyglot)
  modules(libs.graalvm.espresso.language)
  modules(libs.graalvm.espresso.hotswap)

  // GraalVM: LLVM
  modules(libs.graalvm.polyglot.llvm)
  modules(libs.graalvm.llvm.api)
  modules(libs.graalvm.llvm.language)
  modules(libs.graalvm.llvm.language.enterprise)
  modules(libs.graalvm.llvm.language.nfi)
  modules(libs.graalvm.llvm.language.native)
  modules(libs.graalvm.llvm.language.native.resources)
  modules(libs.graalvm.llvm.language.managed)
  modules(libs.graalvm.llvm.language.managed.resources)

  // GraalVM: Truffle
  modules(libs.graalvm.truffle.api)
  modules(libs.graalvm.truffle.processor)
  modules(libs.graalvm.truffle.nfi)
  modules(libs.graalvm.truffle.nfi.panama)
  modules(libs.graalvm.truffle.nfi.libffi)
  modules(libs.graalvm.truffle.nfi.native.linux.amd64)
  modules(libs.graalvm.truffle.nfi.native.darwin.aarch64)

  // General
  implementation(libs.snakeyaml)
  implementation(libs.jansi)
  implementation(libs.jline.graal)
  implementation(libs.jline.console)
  implementation(libs.jline.terminal.jansi)
  implementation(libs.jline.terminal.jni)
  implementation(libs.jline.terminal.jna)
  implementation(libs.jline.terminal.ffm)

  // GraalVM: SVM
  compileOnly(libs.graalvm.svm)

  // Logging
  api(mn.slf4j.api)
  api(mn.slf4j.ext)
  api(mn.slf4j.jcl.over.slf4j)
  api(mn.slf4j.jul.to.slf4j)
  api(mn.slf4j.log4j.over.slf4j)
  runtimeOnly(mn.logback.core)
  runtimeOnly(mn.logback.classic)

  // Crypto
  runtimeOnly(libs.bouncycastle.tls)
  runtimeOnly(libs.bouncycastle.pkix)
  runtimeOnly(libs.bouncycastle.util)
  implementation(libs.conscrypt)

  // Dependencies: Test
  testImplementation(projects.packages.test)
  testImplementation(libs.testing.faker)
  testImplementation(libs.testing.mockito.junit)
  testImplementation(libs.testing.hamcrest)
  testImplementation(libs.testing.hamcrest.junit)
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
  testImplementation(libs.truth.re2j)
  testImplementation(libs.truth.proto)
  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.grpc.testing)
  testCompileOnly(libs.graalvm.svm)
}

graalvmNative {
  toolchainDetection = false
  testSupport = true

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
      inputTaskNames.addAll(listOf("run", "optimizedRun"))
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting = true
    }
  }

  binaries {
    named("main") {
      imageName = if (quickbuild) "elide-embedded.debug" else "elide-embedded"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = true
      buildArgs.addAll(nativeImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs).plus(
        nativeCompileJvmArgs
      ))
    }

    named("optimized") {
      imageName = "elide-embedded"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = true
      buildArgs.addAll(nativeImageArgs(debug = false, release = true, platform = targetOs))
      classpath = files(tasks.optimizedNativeJar, configurations.runtimeClasspath)
    }

    named("test") {
      imageName = "elide-embedded.test"
      fallback = false
      quickBuild = true
      buildArgs.addAll(nativeImageArgs(test = true, debug = false, release = false, platform = targetOs).plus(
        nativeCompileJvmArgs
      ).plus(listOf(
        "-DtestDiscovery",
        "-J-DtestDiscoveryMode",
      )))
    }

    create("entry") {
      imageName = "elide-embedded"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = false
      mainClass = entrypoint
      buildArgs.addAll(nativeImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs))
      classpath = files(tasks.jar, configurations.runtimeClasspath)
    }
  }
}

fun nativeImageArgs(
  platform: String = "generic",
  target: String = "glibc",
  debug: Boolean = quickbuild,
  test: Boolean = false,
  release: Boolean = (!quickbuild && !test && (properties["elide.release"] == "true" || properties["buildMode"] == "release")),
): List<String> =
  commonNativeArgs.asSequence().plus(
    jvmCompileArgs,
  ).plus(
    jvmCompileArgs.map { "-J$it" },
  ).plus(
    initializeAtBuildTime.map { "--initialize-at-build-time=$it" },
  ).plus(
    initializeAtRuntime.map { "--initialize-at-run-time=$it" },
  ).plus(
    rerunAtRuntime.map { "--rerun-class-initialization-at-runtime=$it" },
  ).plus(
    when (platform) {
      "windows" -> if (release) windowsReleaseArgs else windowsOnlyArgs
      "darwin" -> if (release) darwinReleaseArgs else darwinOnlyArgs
      "linux" -> if (target == "musl") muslArgs else (if (release) linuxReleaseArgs else linuxOnlyArgs)
      else -> defaultPlatformArgs
    },
  ).plus(
    if (test) {
      testOnlyArgs.plus(
        initializeAtBuildTimeTest.map {
          "--initialize-at-build-time=$it"
        },
      ).plus(
        initializeAtRuntimeTest.map {
          "--initialize-at-run-time=$it"
        },
      ).plus(
        rerunAtRuntimeTest.map {
          "--rerun-class-initialization-at-runtime=$it"
        },
      )
    } else {
      emptyList()
    },
  ).plus(
    jvmDefs.map { "-D${it.key}=${it.value}" },
  ).plus(
    hostedRuntimeOptions.map { "-H:${it.key}=${it.value}" },
  ).plus(
    if (debug && !release) debugFlags else if (release) releaseFlags else emptyList(),
  ).plus(if (enableStrictHeap && !test) listOf(
    "--strict-image-heap",
  ) else emptyList()).toList()

tasks {
  test {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
  }

  compileJava.configure {
    dependsOn(compileKotlin)
    mustRunAfter(compileKotlin)

    if (enableJpms) {
      options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf("--patch-module", "$module=${compileKotlin.get().destinationDirectory.asFile.get().path}")
      })
    }
  }
}
