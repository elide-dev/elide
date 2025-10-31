/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.internal.conventions.kotlin

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.noarg.gradle.NoArgExtension
import org.jetbrains.kotlin.powerassert.gradle.PowerAssertGradleExtension
import java.util.LinkedList
import java.util.TreeSet
import kotlinx.atomicfu.plugin.gradle.AtomicFUGradlePlugin
import kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension
import elide.internal.conventions.Constants.Elide
import elide.internal.conventions.Constants.Kotlin
import elide.internal.conventions.Constants.Versions
import elide.internal.conventions.ElideBuildExtension
import elide.internal.conventions.jvm.configureJavadoc
import elide.internal.conventions.jvm.includeSourceJar
import elide.internal.conventions.kotlin.KotlinTarget.*
import elide.internal.conventions.publishing.publishJavadocJar

// When operating with a JVM target range, this is the minimum JVM target version.
private const val DEFAULT_JVM_MINIMUM = 8

// When operating with a JVM target range, this is the maximum JVM target version.
private const val DEFAULT_JVM_TARGET = 21

// ID for the Kotlin power assert plugin.
private const val POWER_ASSERT_PLUGIN_ID = "org.jetbrains.kotlin.plugin.power-assert"

// ID for the Kotlin JS typed-plain-objects plugin.
private const val JS_OBJECTS_PLUGIN_ID = "org.jetbrains.kotlin.plugin.js-plain-objects"

// ID for the Kotlin SAM receiver plugin.
private const val SAM_RECEIVER_PLUGIN_ID = "org.jetbrains.kotlin.plugin.sam-with-receiver"

// ID for the Kotlin KAPT plugin.
private const val KAPT_PLUGIN_ID = "org.jetbrains.kotlin.kapt"

// ID for the Kotlin KSP plugin.
private const val KSP_PLUGIN_ID = "com.google.devtools.ksp"

// List of symbols eligible for power-assert processing.
private val powerAssertEligibleSymbols = setOf(
  "kotlin.test.assertEquals",
  "kotlin.test.assertTrue",
  "kotlin.test.assertFalse",
  "kotlin.test.assertNotNull",
  "kotlin.test.assertNull",
  "kotlin.test.assertSame",
  "kotlin.test.assertNotSame",
)

/**
 * Configure a Kotlin project targeting a specific platform. Options passed to this convention are applied to every
 * Kotlin source set.
 *
 * @param target Module targeting.
 * @param conventions Conventions to apply when configuring Kotlin.
 * @param configureJavaModules Whether to enable processing of `module-info.java` files for JPMS support.
 * @param configureMultiReleaseJar Whether to create a multi-release JAR; must pass [configureJavaModules] as `true`.
 * @param javaTargetRange The range of Java versions to target for multi-release JARs; calculated from target & minimum.
 */
@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal fun Project.configureKotlinBuild(
  target: KotlinTarget,
  conventions: ElideBuildExtension.Kotlin,
  configureJavaModules: Boolean = false,
  javaMinimum: Int = DEFAULT_JVM_MINIMUM,
  javaTarget: Int = DEFAULT_JVM_TARGET,
  configureMultiReleaseJar: Boolean = false,
  javaTargetRange: IntRange = javaMinimum..javaTarget,
  jvmModuleName: String? = null,
) {
  val kotlinVersion = conventions.kotlinVersionOverride ?: findProperty(Versions.KOTLIN)?.toString()
  val strictModeEligible = conventions.strict
  val strictModeActive = (
    findProperty(Kotlin.STRICT_MODE).toString().toBoolean() ||
    findProperty(Kotlin.STRICT_MODE_ALT).toString().toBoolean()
  )
  val useStrictMode = strictModeEligible && strictModeActive

  // Maven Central requires a javadoc JAR artifact
  configureJavadoc()
  publishJavadocJar()

  // configure Dokka to depend on code generation tasks
  configureDokka()

  if (conventions.atomicFu) {
    apply(plugin = "kotlinx-atomicfu")
    pluginManager.apply(AtomicFUGradlePlugin::class.java)
    pluginManager.withPlugin("kotlinx-atomicfu") {
      the<AtomicFUPluginExtension>().apply {
        dependenciesVersion = null
        transformJvm = true
        jvmVariant = "VH"
      }
    }
  }

  // Multiplatform targets have a few extra settings
  if (target !is JVM) {
    configureKotlinMultiplatform(
      target,
      configureJavaModules,
      conventions.splitJvmTargets,
      conventions.nonJvmSourceSet,
      conventions.jvmSourceSet,
      jvmModuleName,
      javaMinimum,
      javaTarget,
      configureMultiReleaseJar,
      javaTargetRange,
    )
  } else {
    // configure sources JAR generated by the java plugin (only for pure Kotlin/JVM projects)
    includeSourceJar()

    // configure kotlin for jvm
    configureKotlinJvm(
      target,
      configureJavaModules,
      conventions.splitJvmTargets,
      jvmModuleName,
      javaMinimum,
      javaTarget,
      configureMultiReleaseJar,
      javaTargetRange,
    )
  }

  val enableKapt = conventions.kapt || plugins.hasPlugin(KAPT_PLUGIN_ID)
  val enableKsp = conventions.ksp || plugins.hasPlugin(KSP_PLUGIN_ID)

  val kotlinVersionParsed = KotlinVersion.fromVersion(kotlinVersion ?: Versions.KOTLIN_DEFAULT)
  fun <T: KotlinCommonCompilerOptions> T.configureCompilerOptions() {
    apiVersion.set(kotlinVersionParsed)
    languageVersion.set(kotlinVersionParsed)
    allWarningsAsErrors.set(useStrictMode)
    progressiveMode.set(kotlinVersion != "2.0" && useStrictMode)  // progressive mode makes no sense for bleeding-edge

    if (this is KotlinJvmCompilerOptions) {
      javaParameters.set(true)
    }

    val kotlinCArgs = when (target) {
      JVM -> if (enableKapt) Elide.KaptCompilerArgs else Elide.JvmCompilerArgs
      JsBrowser, JsNode -> Elide.JsCompilerArgs
      is Multiplatform, Native, NativeEmbedded, WASM, WASI -> when {
        target.contains(JVM) && this is KotlinJvmCompilerOptions ->
          Elide.KmpCompilerArgs.plus(if (enableKapt) Elide.KaptCompilerArgs else Elide.JvmCompilerArgs).toSet().toList()

        else -> Elide.KmpCompilerArgs
      }
    }.plus(conventions.customKotlinCompilerArgs).toList()

    freeCompilerArgs.convention(kotlinCArgs)

    val currentSuite = TreeSet(freeCompilerArgs.get())
    val additions = LinkedList<String>()
    kotlinCArgs.forEach {
      if (!currentSuite.contains(it)) {
        currentSuite.add(it)
        additions.add(it)
      }
    }
    if (additions.isNotEmpty()) {
      freeCompilerArgs.addAll(additions)
    }
  }

  // base Kotlin options
  val multiplatformExtension = extensions.findByType(KotlinMultiplatformExtension::class.java)
  val projectExtension = extensions.findByType(KotlinProjectExtension::class.java)

  fun KotlinProjectExtension.configureKotlinProject() {
    sourceSets.apply {
      if (conventions.wasmSourceSets && !isWasmDisabled()) {
        val wasmMain = create("wasmMain") {
          findByName("commonMain")?.let { dependsOn(it) }
        }
        val wasmTest = create("wasmTest") {
          findByName("commonTest")?.let { dependsOn(it) }
        }
        (findByName("wasmJsMain") ?: create("wasmJsMain")).apply {
          dependsOn(wasmMain)
        }
        (findByName("wasmJsTest") ?: create("wasmJsTest")).apply {
          dependsOn(wasmTest)
        }
        (findByName("wasmWasiMain") ?: create("wasmWasiMain")).apply {
          dependsOn(wasmMain)
        }
        (findByName("wasmWasiTest") ?: create("wasmWasiTest")).apply {
          dependsOn(wasmTest)
        }
      }
    }

    sourceSets.all {
      languageSettings {
        optIn("kotlin.ExperimentalUnsignedTypes")
        optIn("kotlin.time.ExperimentalTime")
        optIn("elide.runtime.core.DelicateElideApi")

        if (conventions.explicitApi) explicitApi()
        progressiveMode = false
        apiVersion = kotlinVersionParsed.version
        languageVersion = kotlinVersionParsed.version
      }
    }
  }

  when {
    multiplatformExtension != null -> multiplatformExtension.apply {
      configureKotlinProject()
      compilerOptions {
        configureCompilerOptions()
      }
    }

    projectExtension != null -> projectExtension.apply {
      configureKotlinProject()
    }

    else -> {
      logger.warn("No Kotlin project extension found; skipping Elide conventions")
    }
  }

  tasks.withType(KotlinJvmCompile::class.java).configureEach {
    jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING)
  }

  // Kotlin compilation tasks
  tasks.withType(KotlinCompilationTask::class.java).configureEach {
    compilerOptions {
      configureCompilerOptions()
    }
  }

  // configure kapt extension
  if (enableKapt) {
    pluginManager.withPlugin(KAPT_PLUGIN_ID) {
      extensions.getByType(KaptExtension::class.java).apply {
        useBuildCache = true
        strictMode = true
        correctErrorTypes = true
        keepJavacAnnotationProcessors = true
        includeCompileClasspath = false
      }
      afterEvaluate {
        tasks.withType(KotlinJvmCompile::class.java).configureEach {
          if (name.lowercase().contains("kapt")) {
            compilerOptions {
              configureCompilerOptions()
            }
          }
        }
        tasks.withType(KaptGenerateStubsTask::class.java).configureEach {
          compilerOptions {
            configureCompilerOptions()
          }
        }
      }
    }
  }

  // configure KSP extension
  if (enableKsp) pluginManager.withPlugin(KSP_PLUGIN_ID) {
    extensions.getByType(KspExtension::class.java).apply {
      allWarningsAsErrors = useStrictMode
    }

    afterEvaluate {
      tasks.findByName("generateBuildConfig")?.let { genBuildConfig ->
        tasks.findByName("kspKotlin")?.apply {
          dependsOn(genBuildConfig)
        }
      }
    }
  }

  // configure AllOpen plugin
  if (conventions.allOpen) extensions.getByType(AllOpenExtension::class.java).apply {
    annotation("io.micronaut.aop.Around")
  }

  // configure NoArgs plugin
  if (conventions.noArgs) extensions.getByType(NoArgExtension::class.java).apply {
    annotation("elide.annotations.Model")
  }

  // configure `js-plain-objects` plugin
  if (conventions.jsObjects) plugins.apply(JS_OBJECTS_PLUGIN_ID).also {
    pluginManager.withPlugin(JS_OBJECTS_PLUGIN_ID) {
      // nothing at this time
    }
  }

  // configure `power-assert` plugin
  if (conventions.powerAssert) plugins.apply(POWER_ASSERT_PLUGIN_ID).also {
    pluginManager.withPlugin(POWER_ASSERT_PLUGIN_ID) {
      extensions.getByType(PowerAssertGradleExtension::class.java).apply {
        functions.addAll(powerAssertEligibleSymbols)
      }
    }
  }

  // configure `sam-with-receiver` plugin
  if (conventions.samWithReceiver) plugins.apply(SAM_RECEIVER_PLUGIN_ID).also {
    pluginManager.withPlugin(SAM_RECEIVER_PLUGIN_ID) {
      // nothing at this time
    }
  }
}

/** Configure Dokka tasks to depend on KAPT or KSP generation tasks. */
internal fun Project.configureDokka() {
  // dokka should run after KAPT tasks are done (dokkaHtml for KMP projects, dokkaJavadoc for pure JVM)
  if (plugins.hasPlugin("org.jetbrains.kotlin.kapt")) {
    val kaptTasks = tasks.withType(KaptTask::class.java)
    tasks.findByName("dokkaHtml")?.dependsOn(kaptTasks)
    tasks.findByName("dokkaHtmlPartial")?.dependsOn(kaptTasks)
    tasks.findByName("dokkaJavadoc")?.dependsOn(kaptTasks)
  }

  // same principle applies to KSP tasks
//  if (plugins.hasPlugin("com.google.devtools.ksp")) {
//    val kspTasks = tasks.withType(KspTask::class.java)
//    tasks.findByName("dokkaHtml")?.dependsOn(kspTasks)
//    tasks.findByName("dokkaHtmlPartial")?.dependsOn(kspTasks)
//  }

  // if dokka is applied, we must depend on C-interop tasks
  if (plugins.hasPlugin("org.jetbrains.dokka")) {
    tasks.findByName("dokkaHtml")?.apply {
      tasks.findByName("transformNativeMainCInteropDependenciesMetadataForIde")?.let {
        dependsOn(it)
      }
    }
  }
}
