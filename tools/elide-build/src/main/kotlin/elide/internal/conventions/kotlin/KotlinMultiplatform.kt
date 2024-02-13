/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import elide.internal.conventions.jvm.configureJavaModularity
import elide.internal.conventions.kotlin.KotlinTarget.*

/**
 * Configure a Kotlin Multiplatform project.
 *
 * If the [target] includes Kotlin/JVM, the [configureJavaModules] argument controls whether JPMS build is enabled.
 * Maven publications are automatically configured for each target in multiplatform projects.
 */
@OptIn(ExperimentalWasmDsl::class)
internal fun Project.configureKotlinMultiplatform(
  target: KotlinTarget,
  configureJavaModules: Boolean,
) {
  // quick sanity check (JVM is not allowed as a pure target, only as part of a KMP target)
  require(target !is JVM) { "Kotlin JVM target should use the Multiplatform plugin." }

  extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
    // add JVM target
    if (JVM in target) jvm {
      withJava()
      
      // java modules support
      if(configureJavaModules) configureJavaModularity()

      // use JUnit5 runner
      testRuns.getByName("test").executionTask.configure { useJUnitPlatform() }
    }

    // add JS targets
    if (JsBrowser in target || JsNode in target) js {
      // common options
      generateTypeScriptDefinitions()
      compilations.all {
        kotlinOptions {
          sourceMap = true
          moduleKind = "umd"
          metaInfo = true
        }
      }

      if (JsNode in target) nodejs()
      if (JsBrowser in target) browser()
    }

    if (WASM in target) {
      wasmJs {
        nodejs()
        browser()
        d8()

        sourceSets.apply {
          // optional WASM source sets
          findByName("wasmMain")?.apply { dependsOn(getByName("commonMain")) }
          findByName("wasmTest")?.apply { dependsOn(getByName("commonTest")) }
          findByName("wasmJsMain")?.apply { dependsOn(getByName("commonMain")) }
          findByName("wasmJsTest")?.apply { dependsOn(getByName("commonTest")) }
        }
      }
      wasmWasi {
        nodejs()

        sourceSets.apply {
          // optional WASM source sets
          findByName("wasmWasiMain")?.apply { dependsOn(getByName("commonMain")) }
          findByName("wasmWasiTest")?.apply { dependsOn(getByName("commonTest")) }
        }
      }
    }

    // add native targets
    if (Native in target) registerNativeTargets(this@configureKotlinMultiplatform)

    // main host publication lock (for KMP projects targeting platforms without cross-compilation)
    if (project.findProperty("publishMainHostLock") == "true") {
      // the "Main Host" (e.g. a Linux-based GitHub runner) will publish the JVM, JS, and Common targets,
      // other hosts will not configure these publications, instead pushing only their platform-specific components
      val publicationsFromMainHost = listOf(jvm(), js()).map { it.name } + "kotlinMultiplatform"

      project.extensions.getByType(PublishingExtension::class.java).publications.matching {
        it.name in publicationsFromMainHost
      }.all {
        tasks.withType(AbstractPublishToMaven::class.java)
          .matching { it.publication == this@all }
          .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
      }
    }
  }
}

private fun KotlinMultiplatformExtension.registerNativeTargets(project: Project) {
  // add all basic native targets for both architecture families (mingw not available for ARM)
  linuxX64()

  macosX64()
  macosArm64()
  if (project.properties["buildMingw"] != "false") {
    mingwX64()
  }

  sourceSets.apply {
    val commonMain = getByName("commonMain").apply {
      dependencies {
        implementation(kotlin("stdlib"))
      }
    }
    val commonTest = getByName("commonTest").apply {
      dependencies { implementation(kotlin("test")) }
    }
    
    // add an intermediate common target for native
    val nativeMain = create("nativeMain") { dependsOn(commonMain) }
    val nativeTest = create("nativeTest") { dependsOn(commonTest) }

    if (project.properties["buildMingw"] != "false") {
      named("mingwX64Main") { dependsOn(nativeMain) }
      named("mingwX64Test") { dependsOn(nativeTest) }
    }
    named("linuxX64Main") { dependsOn(nativeMain) }
    named("linuxX64Test") { dependsOn(nativeTest) }
    
    named("macosX64Main") { dependsOn(nativeMain) }
    named("macosX64Test") { dependsOn(nativeTest) }
    
    named("macosArm64Main") { dependsOn(nativeMain) }
    named("macosArm64Test") { dependsOn(nativeTest) }
  }
}
