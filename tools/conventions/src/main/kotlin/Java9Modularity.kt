/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 *
 * Modified for use with Elide by Sam Gammon on Tues., May 9th, 2023.
 */

import org.gradle.api.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.compile.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.*

object Java9Modularity {
  private val DEFAULT_JAVA_TARGET_VERSION: JavaVersion = JavaVersion.VERSION_11

  @JvmStatic
  @JvmOverloads
  fun Project.configureJava9ModuleInfo(
    multiRelease: Boolean = true,
    fullBuild: Boolean = false,
    targetVersion: JavaVersion = DEFAULT_JAVA_TARGET_VERSION,
  ) = prepMultiReleaseJar(
    multiRelease,
    fullBuild = fullBuild,
    targetVersion,
  )

  private fun versionNumber(version: JavaVersion): Int = when (version) {
    JavaVersion.VERSION_1_8 -> 8
    JavaVersion.VERSION_1_9 -> 9
    JavaVersion.VERSION_1_10 -> 10
    JavaVersion.VERSION_11 -> 11
    JavaVersion.VERSION_12 -> 12
    JavaVersion.VERSION_13 -> 13
    JavaVersion.VERSION_14 -> 14
    JavaVersion.VERSION_15 -> 15
    JavaVersion.VERSION_16 -> 16
    JavaVersion.VERSION_17 -> 17
    JavaVersion.VERSION_18 -> 18
    JavaVersion.VERSION_19 -> 19
    JavaVersion.VERSION_20 -> 20
    else -> error("Unrecognized Java version '${version.name}'")
  }

  @JvmStatic
  @Suppress("DEPRECATION")
  internal fun Project.prepMultiReleaseJar(
    multiRelease: Boolean,
    fullBuild: Boolean = false,
    vararg targetVersions: JavaVersion,
  ) {
    val kotlin = extensions.findByType<KotlinProjectExtension>() ?: return
    val jvmTargets = kotlin.targets.filter { it is KotlinJvmTarget || it is KotlinWithJavaTarget<*, *> }

    val jvmApiBuild = if (fullBuild) try {
      tasks.getByName<Task>("jvmApiBuild")
    } catch (err: Throwable) { null } else null

    val jvmTest = if (fullBuild) try {
      tasks.getByName<Task>("jvmTest")
    } catch (err: Throwable) { null } else null

    if (jvmTargets.isEmpty()) {
      logger.lifecycle("No Kotlin JVM targets found, can't configure compilation of module-info!")
    }
    jvmTargets.forEach { target ->
      val artifactTask = tasks.getByName<Jar>(target.artifactsTaskName) {
        if (multiRelease) {
          manifest {
            attributes("Multi-Release" to true)
          }
        }
      }

      targetVersions.forEach { targetVersion ->
        target.compilations.forEach { compilation ->
          val compileKotlinTask = compilation.compileKotlinTask as KotlinCompile
          val defaultSourceSet = compilation.defaultSourceSet

          // derive the names of the source set and compile module task
          val sourceSetName = defaultSourceSet.name + "Java${versionNumber(targetVersion)}"
          val compileModuleTaskName = compileKotlinTask.name + "Java${versionNumber(targetVersion)}"

          kotlin.sourceSets.create(sourceSetName) {
            val sourceFile = defaultSourceSet.kotlin.find { it.name == "module-info.java" }
              ?: this.kotlin.find { it.name == "module-info.java" }
            // only configure the compilation if necessary
            if (sourceFile != null || fullBuild) {
              // the default source set depends on this new source set
              if (!fullBuild) defaultSourceSet.dependsOn(this) else dependsOn(defaultSourceSet)

              // register a new compile module task if a module is defined
              if (sourceFile != null && !fullBuild) {
                val targetFile = compileKotlinTask.destinationDirectory.file("../module-info.class").get().asFile

                val compileModuleTask = registerCompileModuleTask(
                  compileModuleTaskName,
                  compileKotlinTask,
                  sourceFile,
                  targetFile,
                  jvmTarget = targetVersion,
                )

                // add the resulting module descriptor to this target's artifact
                artifactTask.dependsOn(compileModuleTask)
                artifactTask.from(targetFile) {
                  if (multiRelease) {
                    into("META-INF/versions/${versionNumber(targetVersion)}/")
                  }
                }
              } else {
                // don't wire together test targets
                if (sourceSetName.lowercase().contains("test")) return@create

                val compileVariantTask = registerCompileVariantTask(
                  compileModuleTaskName,
                  compileKotlinTask,
                  this,
                  targetVersion,
                )

                jvmApiBuild?.dependsOn(compileVariantTask)
                jvmTest?.dependsOn(compileVariantTask)
                artifactTask.dependsOn(compileVariantTask)
                artifactTask.from(compileVariantTask.get().outputs.files) {
                  include("**/*.class")
                  if (multiRelease) {
                    into("META-INF/versions/${versionNumber(targetVersion)}/")
                  }
                }
              }
            } else {
              logger.lifecycle("No module-info.java file found in ${this.kotlin.srcDirs}, can't configure compilation of module-info!")
              // remove the source set to prevent Gradle warnings
              kotlin.sourceSets.remove(this)
            }
          }
        }
      }
    }
  }

  @JvmStatic
  private fun Project.registerCompileModuleTask(
    taskName: String,
    compileTask: KotlinCompile,
    sourceFile: File,
    targetFile: File,
  ) = registerCompileModuleTask(
    taskName,
    compileTask,
    sourceFile,
    targetFile,
    jvmTarget = DEFAULT_JAVA_TARGET_VERSION,
    jvmSource = DEFAULT_JAVA_TARGET_VERSION,
  )

  @Suppress("DuplicatedCode")
  @JvmStatic
  private fun Project.registerCompileModuleTask(
    taskName: String,
    compileTask: KotlinCompile,
    sourceFile: File,
    targetFile: File,
    jvmTarget: JavaVersion,
    jvmSource: JavaVersion = jvmTarget,
  ) = tasks.register(taskName, JavaCompile::class) {
    group = "build"
    description = "Build module for JVM ${jvmTarget.name}"

    // Also add the module-info.java source file to the Kotlin compile task;
    // the Kotlin compiler will parse and check module dependencies,
    // but it currently won't compile to a module-info.class file.
    compileTask.source(sourceFile)

    // Configure the module compile task.
    dependsOn(compileTask)
    source(sourceFile)
    outputs.file(targetFile)
    classpath = files()
    destinationDirectory.set(compileTask.destinationDirectory)
    sourceCompatibility = jvmSource.toString()
    targetCompatibility = jvmTarget.toString()

    doFirst {
      // Provide the module path to the compiler instead of using a classpath.
      // The module path should be the same as the classpath of the compiler.
      options.compilerArgs = listOf(
        "--release", jvmTarget.toString(),
        "--module-path", compileTask.libraries.asPath,
        "-Xlint:-requires-transitive-automatic"
      )
    }

    doLast {
      // Move the compiled file out of the Kotlin compile task's destination dir,
      // so it won't disturb Gradle's caching mechanisms.
      val compiledFile = destinationDirectory.file(targetFile.name).get().asFile
      targetFile.parentFile.mkdirs()
      compiledFile.renameTo(targetFile)
    }
  }

  @Suppress("DuplicatedCode")
  @JvmStatic
  private fun Project.registerCompileVariantTask(
    taskName: String,
    compileTask: KotlinCompile,
    sourceSet: KotlinSourceSet,
    jvmTarget: JavaVersion,
    jvmSource: JavaVersion = jvmTarget,
  ) = tasks.register(taskName, JavaCompile::class) {
    group = "build"
    description = "Build JAR variant classes for JVM ${jvmTarget.name}"

    // Configure the module compile task.
    dependsOn(compileTask)
    source(sourceSet.kotlin.files)
    classpath = files()
    outputs.dir(compileTask.destinationDirectory.dir("META-INF/versions/${versionNumber(jvmTarget)}/"))

    // deps come from parent source set
    destinationDirectory.set(compileTask.destinationDirectory)
    sourceCompatibility = jvmSource.toString()
    targetCompatibility = jvmTarget.toString()
  }
}
