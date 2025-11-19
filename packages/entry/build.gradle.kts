import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom
import proguard.gradle.ProGuardTask

plugins {
  java
  application
  kotlin("jvm")
  kotlin("plugin.allopen")
  id("com.github.johnrengelman.shadow")
  id("org.graalvm.buildtools.native")
  id("com.gradleup.gr8") version "0.11.2"
}

buildscript {
  dependencies {
    classpath("com.guardsquare:proguard-gradle:7.6.1")
  }
}

version = "0.1"
group = "elidemin.dev.elide"

val kotlinVersion = properties["versions.kotlin.sdk"] as String
val atomicfuVersion = "0.27.0"
val kotlinxCoroutines = "1.10.1"
val kotlinxSerialization = "1.8.0"
val nettyVersion = "4.1.118.Final"
val micrometerVersion = "1.1.2"
val jacksonVersion = "2.18.2"
val graalvmVersion = "24.1.2"
val pklVersion = "0.28.0-SNAPSHOT"
val cliktVersion = "5.0.3"
val mordantVersion = "3.0.2"
val mosaicVersion = "0.16.0"
val jvmToolchain = 23
val jvmTarget = jvmToolchain
val kotlinTarget = JvmTarget.JVM_23
val javaTarget = JavaLanguageVersion.of(jvmToolchain)
val jvmTargetVersion = JavaVersion.toVersion(jvmTarget)
val march = "x86-64-v3"
val optMode = "s"
val nativeOptMode = "2"

fun JavaToolchainSpec.javaToolchainSuite() {
  languageVersion = javaTarget
  vendor = JvmVendorSpec.ORACLE
}

val enablePgo = true
val enableNativePgo = false
val enablePgoInstrument = false
val enableNativePgoInstrument = false
val enablePkl = false
val enableIsolates = false
val enablePreinit = true
val enablePreinitJs = true
val enableJsIsolates = true
val strict = false
val enableClang = false
val enableLlvm = false
val enableGccEdge = true
val enableSbom = true
val enableDualMinify = true
val enableLld = enableClang
val enableMold = true
val enableMemoryProtectionKeys = false
val enableG1 = false
val enableExperimental = true
val enableTruffle = true
val enableAuxCache = false
val minifierMode = "gr8"
val compilerBackend = if (enableLlvm) "llvm" else "lir"
val gc = if (enableG1 && !(enableAuxCache || enableMemoryProtectionKeys)) "G1" else "serial"
val entrypoint = "elide.entry.MainKt"

val javac = javaToolchains.compilerFor { javaToolchainSuite() }
val java = javaToolchains.launcherFor { javaToolchainSuite() }
val gvmJarsRoot = rootProject.layout.projectDirectory.dir("third_party/oracle")

val patchedLibs = files(
  gvmJarsRoot.file("graaljs.jar"),
  gvmJarsRoot.file("truffle-api.jar"),
)

val patched: Configuration by configurations.creating {
  isCanBeResolved = true
}

val kotlincFlags = listOf(
  "-no-reflect",
  "-no-stdlib",
  "-jvm-default=no-compatibility",
  "-Xjsr305=strict",
  "-Xabi-stability=stable",
  "-Xassertions=jvm",
  "-Xcompile-java",
  "-Xemit-jvm-type-annotations",
  "-Xir-inliner",
  "-Xenhance-type-parameter-types-to-def-not-null",
  "-Xjdk-release=${kotlinTarget.target}",
  "-Xjspecify-annotations=strict",
  "-Xno-call-assertions",
  "-Xno-param-assertions",
  "-Xno-receiver-assertions",
  "-Xsam-conversions=indy",
  "-Xstring-concat=indy-with-constants",
  "-Xuse-fast-jar-file-system",
  "-Xuse-k2-kapt",
  "-Xvalidate-bytecode",
  "-Xvalue-classes",
  "-Xinline-classes",
)

val minifiedConfigurations = listOf(
  "runtimeClasspath",
)

val r8Rules = listOf(
  "common.pro",
  "r8.pro"
)

val proguardRules = listOf(
  "common.pro",
  "proguard.pro"
)

val javacFlags = listOf(
  "--enable-preview",
  "--add-modules=jdk.unsupported",
  "--add-modules=java.net.http",
  "-da",
  "-dsa",
)

val javacOnlyFlags = listOf(
  "-g",
)

val isolates = (when {
  enableIsolates && enableJsIsolates -> "js"
  else -> null
})

val preinitializedContexts = buildList {
  if (enablePreinit && enablePreinitJs) add("js")
}.joinToString(",")

val globalizedJvmDefs = listOf(
  "org.graalvm.supporturl" to "https://elide.dev",
  "org.graalvm.vendor" to "Elide (GraalVM)",
)

val jvmDefs = globalizedJvmDefs.plus(listOf(
  "polyglot.engine.WarnVirtualThreadSupport" to "false",
  "polyglotimpl.DisableVersionChecks" to "false",
  "polyglot.engine.SpawnIsolate" to isolates,
  "polyglot.engine.WarnOptionDeprecation" to "false",
  "polyglot.engine.AllowExperimentalOptions" to "true",
  "polyglot.image-build-time.Cache" to "/tmp/elide-auxcache.bin",
  "polyglot.image-build-time.AllowExperimentalOptions" to "true",
  "elide.binpath.override" to layout.buildDirectory.dir("native/nativeOptimizedCompile/labs").get().asFile.path,
  "elide.pkl" to enablePkl.toString(),
  "elide.isolates" to isolates,
  "elide.preinit" to preinitializedContexts,
  "elide.auxcache" to enableAuxCache.toString(),
  "elide.auxcache.trace" to "false",
)).filter { it.second?.ifEmpty { null }?.ifBlank { null } != null }.toMap().plus(if (enablePreinit) mapOf(
  "polyglot.engine.PreinitializeContexts" to preinitializedContexts,
  "polyglot.image-build-time.PreinitializeContexts" to preinitializedContexts,
  "polyglot.image-build-time.PreinitializeContextsWithNative" to "true",
) else emptyMap())

val jvmFlags = javacFlags.plus(listOf(
  "--enable-native-access=ALL-UNNAMED",
  "-XX:+UnlockExperimentalVMOptions",
  //
)).plus(jvmDefs.map {
  "-D${it.key}=${it.value}"
}).plus(
  if (jvmToolchain > 24) listOf(
    "-XX:+UseCompactObjectHeaders",
  ) else emptyList()
)

val initializeAtRuntime: List<String> = listOf(
  // "kotlin.coroutines.jvm.internal.BaseContinuationImpl",
  "org.pkl.core.runtime.VmUtils",
  "org.pkl.core.runtime.VmMapping\$EmptyHolder",
  "org.pkl.core.runtime.VmDynamic\$EmptyHolder",
  "org.pkl.core.runtime.BaseModule",
  "org.pkl.core.runtime.BaseModule\$BooleanClass",
  "org.pkl.core.runtime.BaseModule\$MappingClass",
  "org.pkl.core.runtime.BaseModule\$ListingClass",
  "org.pkl.core.runtime.BaseModule\$ListClass",
  "org.pkl.core.runtime.BaseModule\$DynamicClass",
  "org.pkl.core.runtime.BaseModule\$FloatClass",
  "org.pkl.core.runtime.BaseModule\$ResourceClass",
  "org.pkl.core.runtime.BaseModule\$ModuleClass",
  "org.pkl.core.runtime.BaseModule\$IntClass",
  "org.pkl.core.runtime.BaseModule\$StringClass",
  "org.pkl.core.runtime.BaseModule\$DataSizeClass",
  "org.pkl.core.runtime.BaseModule\$Function0Class",
  "org.pkl.core.runtime.BaseModule\$Function1Class",
  "org.pkl.core.runtime.BaseModule\$Function2Class",
  "org.pkl.core.runtime.BaseModule\$Function3Class",
  "org.pkl.core.runtime.BaseModule\$Function4Class",
  "org.pkl.core.runtime.BaseModule\$Function5Class",
  "org.pkl.core.runtime.BaseModule\$Function6Class",
  "org.pkl.core.runtime.BaseModule\$AnnotationClass",
  "org.pkl.core.runtime.BaseModule\$ModuleInfoClass",
  "org.pkl.core.runtime.ReflectModule",
  "org.pkl.core.runtime.ReflectModule\$DeclaredTypeClass",
  "org.pkl.core.runtime.BenchmarkModule",
  "org.pkl.core.runtime.JsonnetModule",
  "org.pkl.core.runtime.SemVerModule",
  "org.pkl.core.runtime.AnalyzeModule",
  "org.pkl.core.runtime.ProjectModule",
  "org.pkl.core.runtime.XmlModule",
  "org.pkl.core.runtime.TestModule",
  "org.pkl.core.runtime.SettingsModule",
  "org.pkl.core.stdlib.platform.PlatformNodes\$current",
  "org.pkl.core.stdlib.protobuf.RendererNodes",
  "org.pkl.core.stdlib.analyze.AnalyzeNodes",
)

val excludedClassPaths: List<String> = listOf(
  // "kotlinx.coroutines.debug.internal.AgentPremain",
)

val exclusionPaths: List<String> = emptyList<String>().plus(excludedClassPaths.map {
  it.replace(".", "/").let { path ->
    if (path.lowercase() != path) ("$path.class") else path
  }
})

val nativeCompilerPath =
  if (enableClang) "/usr/bin/clang-18" else {
    if (enableGccEdge) "/usr/bin/gcc-14" else "/usr/bin/gcc"
  }

val nativeLinker = when {
  enableLld || enableClang -> "lld"
  enableMold -> "mold"
  else -> "ld"
}

val stdNativeFlags = listOf(
  "-ffast-math",
  "-fno-omit-frame-pointer",
  "-ffunction-sections",
  "-fdata-sections",
  "-fstack-protector",
)

val clangFlags = stdNativeFlags.plus(listOf())

val gccFlags = stdNativeFlags.plus(listOf(
  "-fmerge-all-constants",
  "-fgcse-sm",
  "-fgcse-las",
  "-funsafe-loop-optimizations",
  "-fno-semantic-interposition",
  "-fipa-pta",
  "-fipa-icf",
  "-fno-fat-lto-objects",
)).plus(
  when {
    enableNativePgoInstrument -> listOf(
      "-fprofile-generate=elide-native.prof",
      "-fprofile-arcs",
    )
    enableNativePgo -> listOf(
      "-fbranch-probabilities",
    )
    else -> emptyList()
  }
)

val nativeCompileFlags = listOf(
  "-flto",
  "-O$nativeOptMode",
  "-fuse-ld=$nativeLinker",
  "-fuse-linker-plugin",
).plus(
  if (enableClang) clangFlags else gccFlags
)

val nativeLinkerFlags = listOf(
  "-flto",
  "--emit-relocs",
  "--gc-sections",
)

val experimentalFlags = listOf(
  "-H:+OptimizeLongJumps",
  "-R:+OptimizeLongJumps",
  "-R:+WriteableCodeCache",
  "-H:LocalizationCompressBundles='.*'",

  "-H:+InlineGraalStubs",
  "-H:+ContextAwareInlining",
  "-H:ExcludeResources=org/graalvm/shadowed/com/ibm/icu/impl/data/icudt74b/.*",

  "-H:+ReduceCodeSize",
  "-H:+ProtectionKeys",
  "-H:CFI=SW_NONATIVE",
  "-H:+ForeignAPISupport",
  "-H:+LocalizationOptimizedMode",
  "-H:CStandard=C11",
).plus(
  if (jvmTarget > 24) listOf(
    "-H:+VectorAPISupport",
    "-H:+MLProfileInferenceUseGNNModel",
  ) else emptyList()
)

val pgoProf = layout.projectDirectory.file("default.iprof").asFile.absolutePath

val gvmFlags = (if (enablePgo) listOf(
  "--pgo=$pgoProf",
) else {
  if (enablePgoInstrument) listOf(
    "--pgo-instrument",
  ) else listOf(
    "-O$optMode",
  )
}).plus(listOf(
  "--gc=$gc",
  "-march=$march",
  "--add-modules=jdk.incubator.vector",
  "--add-modules=jdk.unsupported",
  "--link-at-build-time",
  "--initialize-at-build-time",
  "--color=always",
  "--emit=build-report",
  "-H:+UnlockExperimentalVMOptions",
  "-H:+UseCompressedReferences",
  "-H:+PreserveFramePointer",
  "-H:IncludeResources=org/graalvm/shadowed/com/ibm/icu/ICUConfig.properties",
  "-H:+CopyLanguageResources",
  "--exact-reachability-metadata",
  "--native-compiler-path=$nativeCompilerPath",
)).plus(nativeCompileFlags.map {
  "--native-compiler-options=$it"
}.plus(nativeLinkerFlags.flatMap {
  listOf(
    "--native-compiler-options=-Wl,$it",
  )
})).plus(
  if (enableExperimental) experimentalFlags else emptyList()
).plus(
  if (enableAuxCache) listOf(
    "-H:+AuxiliaryEngineCache",
    "-H:ReservedAuxiliaryImageBytes=107374182",
    "-Dpolyglot.engine.Cache=/tmp/elide-auxcache.bin",
  ) else emptyList()
).plus(
  if (enableSbom) listOf(
    "--enable-sbom=cyclonedx",
  ) else emptyList()
).plus(
  initializeAtRuntime.map {
    "--initialize-at-run-time=$it"
  }
)

val jvmOnly: Configuration by configurations.creating { isCanBeResolved = true }
val pkl: Configuration by configurations.creating { isCanBeResolved = true }
val unshaded: Configuration by configurations.creating { isCanBeResolved = true }

val truffle: Configuration by configurations.creating {
  isCanBeResolved = true
  extendsFrom(patched)
}

fun extendsBaseClasspath(configuration: Configuration) {
  configurations.compileClasspath.extendsFrom(
    configurations.named(configuration.name)
  )
  configurations.runtimeClasspath.extendsFrom(
    configurations.named(configuration.name)
  )
}

extendsBaseClasspath(truffle)
extendsBaseClasspath(unshaded)
extendsBaseClasspath(pkl)

fun ExternalModuleDependency.truffleExclusions() {
  exclude(group = "org.graalvm.truffle")
  exclude(group = "org.graalvm.sdk")
}

dependencies {
  implementation("com.github.ajalt.clikt:clikt:$cliktVersion") {
    exclude(group = "com.github.ajalt.mordant", module = "mordant-jvm-ffm")
  }
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinxCoroutines")
  // implementation("org.jetbrains.kotlinx:atomicfu-jvm:$atomicfuVersion")
  compileOnly("org.graalvm.nativeimage:svm:$graalvmVersion")

  nativeImageCompileOnly("com.github.ajalt.mordant:mordant-jvm-graal-ffi:$mordantVersion")
  jvmOnly("com.github.ajalt.mordant:mordant-jvm-ffm:$mordantVersion")

  truffle("org.graalvm.polyglot:polyglot:$graalvmVersion")
  truffle("org.graalvm.js:js-language:$graalvmVersion")
  truffle("org.graalvm.regex:regex:$graalvmVersion")
  truffle("org.graalvm.nativeimage:truffle-runtime-svm:$graalvmVersion")
  truffle("org.graalvm.truffle:truffle-api:$graalvmVersion")
  truffle("org.graalvm.truffle:truffle-enterprise:$graalvmVersion")
  truffle("org.graalvm.shadowed:icu4j:$graalvmVersion")
  truffle("org.graalvm.polyglot:js:$graalvmVersion")
  truffle("org.graalvm.truffle:truffle-nfi-libffi:$graalvmVersion")
  truffle(patchedLibs)

  if (enableIsolates) {
    truffle("org.graalvm.polyglot:js-isolate:$graalvmVersion")
  }
  if (enablePkl) {
    pkl("org.pkl-lang:pkl-executor:$pklVersion") { truffleExclusions() }
    pkl("org.pkl-lang:pkl-commons-cli:$pklVersion") { truffleExclusions() }
    pkl("org.pkl-lang:pkl-core:$pklVersion") { truffleExclusions() }
    pkl("org.pkl-lang:pkl-cli:$pklVersion") { truffleExclusions() }
    pkl("org.pkl-lang:pkl-stdlib:$pklVersion") { truffleExclusions() }
  }
}

configurations.all {
  exclude("org.pkl-lang", "pkl-server")

  // Exclude isolate libs for platforms we aren't targeting.
  if (!enableIsolates) {
    exclude(group = "org.graalvm.polyglot", module = "js-isolate")
  }
  exclude(group = "org.graalvm.polyglot", module = "js-isolate-linux-aarch64")
  exclude(group = "org.graalvm.polyglot", module = "js-isolate-windows-amd64")
  exclude(group = "org.graalvm.polyglot", module = "js-isolate-darwin-amd64")
  exclude(group = "org.graalvm.polyglot", module = "js-isolate-darwin-aarch64")

  if (name != "jvmOnly") {
    exclude(group = "com.github.ajalt.mordant", module = "mordant-jvm-ffm")
  }
}

application {
  mainClass = entrypoint
}

java {
  sourceCompatibility = jvmTargetVersion
  targetCompatibility = jvmTargetVersion
  toolchain { javaToolchainSuite() }
  modularity.inferModulePath = true
}

kotlin {
  compilerOptions {
    apiVersion = KotlinVersion.KOTLIN_2_1
    languageVersion = KotlinVersion.KOTLIN_2_1
    jvmTarget = kotlinTarget
    javaParameters = true
    allWarningsAsErrors = strict
    freeCompilerArgs = kotlincFlags.plus(javacFlags.plus(javacOnlyFlags).map {
      "-Xjavac-arguments=$it"
    })
  }
}

tasks.shadowJar {
  mergeServiceFiles()

  exclude("module-info.class")
  exclusionPaths.forEach {
    exclude(it)
  }
}

val proguardedJar by tasks.registering(ProGuardTask::class) {
  group = "build"
  description = "Builds the proguarded jar"

  configuration(files(*proguardRules.toTypedArray()))

  val libJars = truffle
    .plus(unshaded)
    .plus(pkl)
    .files
    .distinct()

  val allInJars = listOf(tasks.jar.get().outputs.files.singleFile)
    .plus(configurations.runtimeClasspath.get())
    .filter { it !in libJars }
    .distinct()

  dependsOn(tasks.jar)
  injars(allInJars)
  libraryjars(libJars)
  val out = layout.buildDirectory.file("libs/${project.name}-proguarded.jar").get().asFile
  outjars(out)
  outputs.files(out)
}

val gr8MinifiedJar = gr8.create("minified") {
  r8Rules.forEach { proguardFile(layout.projectDirectory.file(it).asFile) }
  systemClassesToolchain(javac.get())
  r8Version("1.4.45")
  addClassPathJarsFrom(
    provider {
      truffle
        .plus(unshaded)
        .plus(pkl)
        .plus(configurations.runtimeClasspath.get())
        .filter { it.extension != "pom" }
        .distinct()
    }
  )

  if (enableDualMinify) {
    addProgramJarsFrom(proguardedJar)
  } else {
    addProgramJarsFrom(tasks.jar)
  }
}

val minifiedJar by tasks.registering(Copy::class) {
  group = "build"
  description = "Builds the minified jar"
  dependsOn(if (minifierMode == "gr8") gr8MinifiedJar else proguardedJar)
  val minJarFile: File = if (minifierMode == "gr8")
    tasks.named("gr8MinifiedShadowedJar").get()
      .outputs
      .files
      .filter { it.extension == "jar" }
      .filter { !("jetbrains" in it.path && "annotations" in it.path) }
      .singleFile
  else
    proguardedJar.get().outputs.files.singleFile

  inputs.files(minJarFile)
  outputs.files(layout.buildDirectory.file("libs/${minJarFile.name}").get().asFile)

  from(minJarFile.parentFile) {
    include(minJarFile.name)
  }
  into(layout.buildDirectory.dir("libs").get().asFile)
}

graalvmNative {
  toolchainDetection = false

  binaries {
    create("optimized") {
      fallback = false
      richOutput = true
      useArgFile = true
      imageName = "entry"

      buildArgs.addAll(javacFlags
                         .plus(gvmFlags)
                         .plus(entrypoint)
                         .plus(listOf(
                            "--static",
                            "--libc=musl",
                            "--install-exit-handlers",
                            "-R:+InstallSegfaultHandler",
                         )))

      buildArgs.addAll(project.provider {
        listOf(
          "--module-path",
          truffle.filter { it.extension != "pom" }.asPath,
        )
      })
      jvmArgs.addAll(jvmFlags)

      classpath(
        unshaded,
        pkl,
        minifiedJar.get().outputs.files.filter {
          it.extension == "jar"
        }
      )
    }
  
    create("shared") {
      fallback = false
      richOutput = true
      useArgFile = true
      imageName = "libentry"

      buildArgs.addAll(javacFlags
                         .plus(gvmFlags)
                         .plus(listOf("--shared")))

      buildArgs.addAll(project.provider {
        listOf(
          "--module-path",
          truffle.filter { it.extension != "pom" }.asPath,
        )
      })
      jvmArgs.addAll(jvmFlags)

      classpath(
        unshaded,
        pkl,
        minifiedJar.get().outputs.files.filter {
          it.extension == "jar"
        }
      )
    }
  }

  agent {
    defaultMode = "standard"
    builtinCallerFilter = true
    builtinHeuristicFilter = true
    trackReflectionMetadata = true
    enableExperimentalPredefinedClasses = true
    enableExperimentalUnsafeAllocationTracing = true
    enabled = properties.containsKey("agent")
    modes {
      standard {}
    }
    metadataCopy {
      inputTaskNames.addAll(listOf("run"))
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting = true
    }
  }
}

configurations.all {
  listOf(
    "com.github.ajalt.mordant:mordant-jvm-jna",
  ).forEach {
    val (grp, mod) = it.split(":")
    exclude(group = grp, module = mod)
  }

  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(kotlinVersion)
      because("kotlin sdk pin")
    }
    if (requested.group == "org.jetbrains" && requested.name == "annotations") {
      useVersion("23.0.0")
      because("solving duplicates")
    }
  }
}

tasks.test {
  jvmArgs(jvmFlags)
}

tasks.named("run", JavaExec::class) {
  jvmArgs(jvmFlags)
  classpath(configurations.runtimeClasspath, jvmOnly)
  outputs.upToDateWhen { false }
  doNotTrackState("not cacheable")
}

tasks.withType<JavaCompile>().configureEach {
  modularity.inferModulePath = true
  options.compilerArgumentProviders.add(CommandLineArgumentProvider {
    javacOnlyFlags
  })
}

tasks.withType<KotlinJvmCompile>().configureEach {
  jvmTargetValidationMode = JvmTargetValidationMode.WARNING
}

listOf(
  "nativeCompile",
  "nativeOptimizedCompile",
).forEach {
  tasks.named(it).configure { dependsOn(tasks.jar, tasks.shadowJar, minifiedJar, proguardedJar) }
}

tasks.withType<Jar>().configureEach {
  doLast {
    @Suppress("DEPRECATION") exec {
      commandLine("du", "-hac", outputs.files.singleFile.absolutePath)
    }
  }
}

tasks.withType<ProGuardTask>().configureEach {
  doLast {
    @Suppress("DEPRECATION") exec {
      commandLine("du", "-hac", "build/libs")
    }
  }
}

tasks.withType<BuildNativeImageTask>().configureEach {
  doLast {
    @Suppress("DEPRECATION") exec {
      commandLine("du", "-hac", "build/native/$name", "build/libs")
    }
  }
}

tasks.build {
  dependsOn(minifiedJar)

  doLast {
    @Suppress("DEPRECATION") exec {
      commandLine("du", "-hac", "build/libs")
    }
  }
}

listOf(
  "distTar",
  "distZip",
  "startScripts",
  "startShadowScripts",
).forEach {
  tasks.named(it).configure {
    dependsOn("minifiedJar")
  }
}

configurations.all {
  listOf(
    "org.graalvm.js:js-language",
    "org.graalvm.truffle:truffle-api",
  ).forEach {
    exclude(group = it.substringBefore(":"), module = it.substringAfter(":"))
  }
}
