package elide.internal.conventions.kotlin

import com.google.devtools.ksp.gradle.KspTask
import org.gradle.api.Project
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.noarg.gradle.NoArgExtension
import elide.internal.conventions.Constants.Elide
import elide.internal.conventions.Constants.Kotlin
import elide.internal.conventions.Constants.Versions
import elide.internal.conventions.jvm.configureJavadoc
import elide.internal.conventions.kotlin.KotlinTarget.*
import elide.internal.conventions.publishing.publishJavadocJar

/**
 * Configure a Kotlin project targeting a specific platform. Options passed to this convention are applied to every
 * Kotlin source set.
 *
 * @param explicitApi Whether to enable Explicit API mode for all source sets.
 * @param configureKapt Whether to configure KAPT if present, according to internal conventions.
 * @param configureAllOpen Whether to configure the AllOpen plugin with annotations used often in Elide packages.
 * @param configureNoArgs Whether to configure the NoArgs plugin with annotations used often in Elide packages.
 * @param configureJavaModules Whether to enable processing of `module-info.java` files for JPMS support.
 * @param customKotlinCompilerArgs Custom Kotlin compiler args to apply.
 */
internal fun Project.configureKotlinBuild(
  target: KotlinTarget,
  explicitApi: Boolean = false,
  configureKapt: Boolean = false,
  configureAllOpen: Boolean = false,
  configureNoArgs: Boolean = false,
  configureJavaModules: Boolean = false,
  customKotlinCompilerArgs: List<String> = emptyList(),
) {
  val kotlinVersion = findProperty(Versions.KOTLIN)?.toString()
  val kotlinSdk = findProperty(Versions.KOTLIN_SDK)?.toString()
  val useStrictMode = findProperty(Kotlin.STRICT_MODE).toString().toBoolean()

  // Maven Central requires a javadoc JAR artifact
  configureJavadoc()
  publishJavadocJar()

  // configure Dokka to depend on code generation tasks
  configureDokka()

  // Multiplatform targets have a few extra settings
  if (target !is JVM) configureKotlinMultiplatform(target, configureJavaModules)

  // base Kotlin options
  extensions.getByType(KotlinProjectExtension::class.java).apply {
    sourceSets.all {
      languageSettings {
        if (explicitApi) explicitApi()
        progressiveMode = true

        optIn("kotlin.ExperimentalUnsignedTypes")
      }
    }
  }

  // Kotlin compilation tasks
  tasks.withType(KotlinCompile::class.java).configureEach {
    incremental = true
    kotlinOptions {
      apiVersion = kotlinVersion
      languageVersion = kotlinVersion
      allWarningsAsErrors = useStrictMode
      javaParameters = true

      freeCompilerArgs = when (target) {
        JVM -> if (configureKapt) Elide.KaptCompilerArgs else Elide.JvmCompilerArgs
        JsBrowser, JsNode -> Elide.JsCompilerArgs
        is Multiplatform, Native, WASM -> Elide.KmpCompilerArgs
      }
    }
  }

  // configure kapt extension
  if (configureKapt) extensions.getByType(KaptExtension::class.java).apply {
    useBuildCache = true
    strictMode = true
    correctErrorTypes = true
    keepJavacAnnotationProcessors = true
    includeCompileClasspath = false
  }

  // configure AllOpen plugin
  if (configureAllOpen) extensions.getByType(AllOpenExtension::class.java).apply {
    annotation("io.micronaut.aop.Around")
  }

  // configure NoArgs plugin
  if (configureNoArgs) extensions.getByType(NoArgExtension::class.java).apply {
    annotation("elide.annotations.Model")
  }

  // pin stdlib
  configurations.all {
    resolutionStrategy.eachDependency {
      if (requested.group == "org.jetbrains.kotlin" && requested.name.contains("stdlib")) {
        useVersion(kotlinSdk ?: "1.9.10")
        because("pin kotlin stdlib")
      }
    }
  }
}

/** Configure Dokka tasks to depend on KAPT or KSP generation tasks. */
internal fun Project.configureDokka() {
  // dokka should run after KAPT tasks are done (dokkaHtml for KMP projects, dokkaJavadoc for pure JVM)
  if(plugins.hasPlugin("org.jetbrains.kotlin.kapt")) {
    tasks.findByName("dokkaHtml")?.dependsOn(tasks.withType(KaptTask::class.java))
    tasks.findByName("dokkaJavadoc")?.dependsOn("kaptKotlin")
  }

  // same principle applies to KSP tasks
  if(plugins.hasPlugin("com.google.devtools.ksp")) {
    tasks.findByName("dokkaHtml")?.dependsOn(tasks.withType(KspTask::class.java))
  }
}
