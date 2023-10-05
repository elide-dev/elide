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

@file:Suppress("UnstableApiUsage", "unused")

package elide.internal.conventions.jvm

import org.gradle.api.*
import org.gradle.api.attributes.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.compile.*
import org.gradle.kotlin.dsl.*
import org.gradle.work.*
import org.jetbrains.kotlin.gradle.dsl.*

/**
 * This object configures the Java compilation of a JPMS (aka Jigsaw) module descriptor.
 * The source file for the module descriptor is expected at <project-dir>/src/module-info.java.
 *
 * To maintain backwards compatibility with Java 8, the jvm JAR is marked as a multi-release JAR
 * with the module-info being moved to META-INF/versions/9/module-info.class.
 *
 * The Java toolchains feature of Gradle is used to detect or provision a JDK 11,
 * which is used to compile the module descriptor.
 */
internal object Java9Modularity {
  /**
   * Task that patches `module-info.java` and removes `requires kotlinx.atomicfu` directive.
   *
   * To have JPMS properly supported, Kotlin compiler **must** be supplied with the correct `module-info.java`.
   * The correct module info has to contain `atomicfu` requirement because atomicfu plugin kicks-in **after**
   * the compilation process. But `atomicfu` is compile-only dependency that shouldn't be present in the final
   * `module-info.java` and that's exactly what this task ensures.
   *
   * Additionally, we need to perform this same trick for GraalVM modules (`org.graalvm.sdk` and `org.graalvm.truffle`),
   * which are provided by the runtime.
   */
  abstract class ProcessModuleInfoFile : DefaultTask() {
    @get:InputFile
    @get:NormalizeLineEndings
    abstract val moduleInfoFile: RegularFileProperty

    @get:OutputFile
    abstract val processedModuleInfoFile: RegularFileProperty

    private val projectPath = project.path

    @TaskAction
    fun process() {
      val sourceFile = moduleInfoFile.get().asFile
      if (!sourceFile.exists()) {
        throw IllegalStateException("$sourceFile not found in $projectPath")
      }
      val outputFile = processedModuleInfoFile.get().asFile
      sourceFile.useLines { lines ->
        outputFile.outputStream().bufferedWriter().use { writer ->
          for (line in lines) {
            if ("kotlinx.atomicfu" in line) continue
            if ("org.graalvm.sdk" in line) continue
            if ("org.graalvm.truffle" in line) continue
            writer.write(line)
            writer.newLine()
          }
        }
      }
    }
  }

  @JvmStatic
  fun configure(project: Project) = with(project) {
    val (target, multiplatform) = when (val kotlin = extensions.getByName("kotlin")) {
      is KotlinJvmProjectExtension -> kotlin.target to false
      is KotlinMultiplatformExtension -> kotlin.targets.getByName("jvm") to true
      else -> throw IllegalStateException("Unknown Kotlin project extension in $project")
    }
    val compilation = target.compilations.getByName("main")

    // Force the use of JARs for compile dependencies, so any JPMS descriptors are picked up.
    // For more details, see https://github.com/gradle/gradle/issues/890#issuecomment-623392772
    configurations.getByName(compilation.compileDependencyConfigurationName).attributes {
      attribute(
        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
        objects.named(LibraryElements::class, LibraryElements.JAR)
      )
    }

    val processModuleInfoFile by tasks.registering(ProcessModuleInfoFile::class) {
      moduleInfoFile = if (multiplatform) {
        file("${project.projectDir}/src/jvmMain/kotlin/module-info.java")
      } else {
        file("${project.projectDir}/src/main/kotlin/module-info.java")
      }
      processedModuleInfoFile = project.layout.buildDirectory.file("generated-sources/module-info-processor/module-info.java")
    }

    val compileJavaModuleInfo = tasks.register("compileModuleInfoJava", JavaCompile::class.java) {
      val plainModuleName = project.name.replace('-', '.') // this module's name
      val moduleName = "elide.$plainModuleName"
      val compileKotlinTask =
        compilation.compileTaskProvider.get() as? org.jetbrains.kotlin.gradle.tasks.KotlinCompile
          ?: error("Cannot access Kotlin compile task ${compilation.compileKotlinTaskName}")
      val targetDir = compileKotlinTask.destinationDirectory.dir("../jpms")

      // Always compile kotlin classes before the module descriptor.
      dependsOn(compileKotlinTask)

      // Add the module-info source file.
      // Note that we use the parent dir and an include filter,
      // this is needed for Gradle's module detection to work in
      // org.gradle.api.tasks.compile.JavaCompile.createSpec
      source(processModuleInfoFile.map { it.processedModuleInfoFile.asFile.get().parentFile })
      val generatedModuleInfoFile = processModuleInfoFile.flatMap { it.processedModuleInfoFile.asFile }
      include { it.file == generatedModuleInfoFile.get() }

      // Set the task outputs and destination directory
      outputs.dir(targetDir)
      destinationDirectory = targetDir

      // Configure JVM compatibility
      sourceCompatibility = JavaVersion.VERSION_11.toString()
      targetCompatibility = JavaVersion.VERSION_11.toString()

      // Patch the compileKotlinJvm output classes into the compilation so exporting packages works correctly.
      val destinationDirProperty = compileKotlinTask.destinationDirectory.asFile
      options.compilerArgumentProviders.add {
        val kotlinCompileDestinationDir = destinationDirProperty.get()
        listOf("--patch-module", "$moduleName=$kotlinCompileDestinationDir")
      }

      // Use the classpath of the compileKotlinJvm task.
      // Also ensure that the module path is used instead of classpath.
      classpath = compileKotlinTask.libraries
      modularity.inferModulePath = true
    }

    tasks.named<Jar>(target.artifactsTaskName) {
      manifest {
        attributes("Multi-Release" to true)
      }
      from(compileJavaModuleInfo) {
        // Include **only** file we are interested in as JavaCompile output also contains some tmp files
        include("module-info.class")
        into("META-INF/versions/11/")
      }
    }
  }
}
