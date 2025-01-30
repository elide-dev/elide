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

package elide.internal.conventions.jvm

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.registerTransform
import org.gradle.testing.base.TestingExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.Companion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*
import elide.internal.conventions.Constants.Build
import elide.internal.conventions.Constants.Versions
import elide.internal.conventions.DependencyPin
import elide.internal.conventions.ElideBuildExtension
import elide.internal.conventions.ModuleConfiguration
import elide.internal.conventions.publishing.publishJavadocJar
import elide.internal.conventions.publishing.publishSourcesJar
import elide.internal.transforms.AutomaticModuleTransform
import elide.internal.transforms.JarMinifier

private val enableTransforms = false

internal val lockedConfigurations = sortedSetOf(
  "classpath",
  "compileClasspath",
  "runtimeClasspath",
)

internal val automaticModuleConfigurations = sortedSetOf(
  "compileClasspath",
  "runtimeClasspath",
)

internal val minifiedConfigurations = listOf<String>()

internal val globallyPinnedVersions: Map<String, Pair<String, String>> = sortedMapOf(
  "com.google.guava:guava" to (Versions.GUAVA to "sensitive compatibility"),
  "com.google.protobuf:protobuf-java" to (Versions.PROTOBUF to "sensitive compatibility"),
  "com.google.protobuf:protobuf-java-util" to (Versions.PROTOBUF to "sensitive compatibility"),
  "com.google.protobuf:protobuf-kotlin" to (Versions.PROTOBUF to "sensitive compatibility"),
  "io.opentelemetry:opentelemetry-bom" to (Versions.OPENTELEMETRY to "sensitive compatibility"),
  "org.apache.groovy:groovy-bom" to (Versions.GROOVY to "critical dependency"),
  "org.jetbrains.kotlin:kotlin-stdlib" to (Versions.KOTLIN_SDK_PIN to "critical dependency"),
  "org.jetbrains.kotlin:kotlin-reflect" to (Versions.KOTLIN_SDK_PIN to "critical dependency"),
  "org.jetbrains.kotlinx:kotlinx-coroutines-bom" to (Versions.COROUTINES to "critical dependency"),
  "org.jetbrains.kotlinx:kotlinx-coroutines-core" to (Versions.COROUTINES to "critical dependency"),
  "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm" to (Versions.COROUTINES to "critical dependency"),
  "org.jetbrains.kotlinx:atomicfu" to (Versions.ATOMICFU to "critical dependency"),
)

internal val bannedDependencies: SortedSet<String> = sortedSetOf()

internal val unverifiedConfigurations = emptyList<String>()

internal val defaultModuleTransforms = sortedMapOf(
  "protobuf-java" to ModuleConfiguration.of { forceClasspath = true },
  "protobuf-java-util" to ModuleConfiguration.of { forceClasspath = true },
  "protobuf-kotlin" to ModuleConfiguration.of { forceClasspath = true },
)

/** Apply base Java options to the project. */
@Suppress("UnstableApiUsage")
internal fun Project.configureJava() {
  tasks.withType(JavaCompile::class.java).configureEach {
    options.isFork = true
    options.isIncremental = true
  }

  extensions.findByType(TestingExtension::class.java)?.apply {
    (suites.getByName("test") as JvmTestSuite).useJUnitJupiter()
  }
}

/** Include the "javadoc" JAR in the Java compilation if 'buildDocs' is enabled. */
internal fun Project.includeJavadocJar() {
  val buildDocs = findProperty(Build.BUILD_DOCS)?.toString()?.toBoolean() ?: true

  extensions.getByType(JavaPluginExtension::class.java).apply {
    if (buildDocs) withJavadocJar()
  }

  // attempt to include in publications (only if the extension is applied)
  configureJavadoc()
  publishJavadocJar()
}

/** Apply dependency pinning rules. */
internal fun Project.configurePinnedDependencies(conventions: ElideBuildExtension) {
  configurations.configureEach {
    resolutionStrategy.apply {
      // always prefer project modules
      preferProjectModules()

      // @TODO: re-enable strict deps once gvm snapshot version is not in use
      if (
        !conventions.deps.locking &&
        !gradle.startParameter.isWriteDependencyLocks &&
        findProperty("elide.lockDeps")?.toString()?.toBoolean() != true
      ) {
        // require reproducible resolution
//        failOnNonReproducibleResolution()
      }
      if (conventions.deps.strict) {
        // fail eagerly on version conflict (includes transitive dependencies)
        failOnVersionConflict()
//        failOnChangingVersions()
      } else {
        // allow module caching
        cacheDynamicVersionsFor(7, "days")
        cacheChangingModulesFor(7, "days")
      }
      if (conventions.deps.pinning) {
        eachDependency {
          val coordinate = "${requested.group}:${requested.name}"
          val globallyPinnedVersion = globallyPinnedVersions[coordinate]
          val projectPinnedVersion = conventions.deps.pins.resolve(requested)
          val pinnedVersion = if (projectPinnedVersion != null) {
            projectPinnedVersion to "project conventions"
          } else if (globallyPinnedVersion != null) {
            val (pinValue, pinReason) = globallyPinnedVersion
            DependencyPin.of(requested.group, requested.name, pinValue, pinReason) to "global conventions"
          } else null

          if (pinnedVersion != null) {
            val (pin, reason) = pinnedVersion
            useVersion(pin.version)
            because("pinned by: $reason (reason: ${pin.reason ?: "none given"})")
          }
        }
      }
    }

    // handle globally excluded dependencies
    bannedDependencies.forEach {
      exclude(group = it.substringBefore(":"), module = it.substringAfter(":"))
    }

    // and any additions for this project
    conventions.deps.exclusions.drain().map {
      exclude(group = it.group, module = it.module)
    }
  }
}

/** Apply dependency locking and verification rules. */
internal fun Project.configureDependencySecurity(conventions: ElideBuildExtension) {
  if (conventions.deps.verification) {
    configureDependencyVerification(conventions)
  }
  if (conventions.deps.locking || findProperty(Build.LOCK_DEPS)?.toString()?.toBoolean() != false) {
    configureDependencyLocking(conventions)
  }
}

/** Apply dependency verification rules. */
@Suppress("UNUSED_PARAMETER")
private fun Project.configureDependencyVerification(conventions: ElideBuildExtension) {
  configurations.apply {
    unverifiedConfigurations.forEach {
      findByName(it)?.apply {
        resolutionStrategy.disableDependencyVerification()
      }
    }
  }
}

/** Apply dependency locking rules. */
@Suppress("UNUSED_PARAMETER")
private fun Project.configureDependencyLocking(conventions: ElideBuildExtension) {
  configurations.apply {
    lockedConfigurations.forEach {
      findByName(it)?.apply {
        resolutionStrategy.activateDependencyLocking()
      }
    }
  }
}

/** Include the "sources" JAR in the Java compilation. */
internal fun Project.includeSourceJar() {
  extensions.getByType(JavaPluginExtension::class.java).apply {
    withSourcesJar()
  }

  // attempt to include in publications (only if the extension is applied)
  publishSourcesJar()
}

/** Align JVM target versions between Java and Kotlin compilation tasks. */
internal fun Project.alignJvmVersion(overrideVersion: String? = null) {
  val targetJvmVersion = overrideVersion
    ?: findProperty(Versions.JVM_TARGET)?.toString()
    ?: error("JVM target not set")

  val parsedJvmVersion = JavaVersion.toVersion(targetJvmVersion)
  val parsedJvmTarget = JvmTarget.fromTarget(targetJvmVersion)
  extensions.getByType(JavaPluginExtension::class.java).apply {
    sourceCompatibility = parsedJvmVersion
    targetCompatibility = parsedJvmVersion
  }
  tasks.apply {
    withType(JavaCompile::class.java).configureEach {
      sourceCompatibility = targetJvmVersion
      targetCompatibility = targetJvmVersion

      options.isFork = true
      options.isIncremental = true
    }
    withType(KotlinCompile::class.java).configureEach {
      incremental = true

      compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(targetJvmVersion))
        javaParameters.set(true)
      }
    }
    withType(KotlinCompilationTask::class.java).configureEach {
      compilerOptions {
        if (this is KotlinJvmCompilerOptions) {
          jvmTarget.set(parsedJvmTarget)
          javaParameters.set(true)
        }
      }
    }
  }
}

/** Registers or configures the Javadoc JAR task. */
internal fun Project.configureJavadoc() {
  val buildDocs = findProperty(Build.BUILD_DOCS)?.toString()?.toBoolean() ?: true
  if (!buildDocs) return

  // resolve or create the task
  if (tasks.findByName("dokkaHtml") != null) {
    tasks.maybeCreate("javadocJar", Jar::class.java).apply {
      archiveClassifier.set("javadoc")

      isPreserveFileTimestamps = false
      isReproducibleFileOrder = true

      from(tasks.named("dokkaHtml"))
    }

    tasks.withType(Javadoc::class.java).configureEach {
      isFailOnError = false
    }
  } else tasks.register("javadocJar", Jar::class.java) {
    // create empty javadoc jar
    archiveClassifier.set("javadoc")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
  }
}

public val artifactType: Attribute<String> = Attribute.of("artifactType", String::class.java)
public val minified: Attribute<String> = Attribute.of("minified", String::class.java)
public val modularized: Attribute<String> = Attribute.of("modularized", String::class.java)

/** Configure dependency attribute schema. */
internal fun Project.configureAttributeSchema(conventions: ElideBuildExtension) {
  if (enableTransforms) {
    dependencies {
      attributesSchema {
        attribute(modularized)
        attribute(minified)
      }
      artifactTypes.findByName("jar")?.apply {
        attributes.attribute(modularized, "nope")
        attributes.attribute(minified, "nope")
      }
    }

    if (conventions.deps.enableTransforms) {
      val allTransformEligibleConfigurations = automaticModuleConfigurations.plus(minifiedConfigurations)
      configurations.all {
        if (name in allTransformEligibleConfigurations) {
          val enableAmr = (conventions.deps.automaticModules && automaticModuleConfigurations.contains(name))
          val enableMin = (conventions.deps.minification && minifiedConfigurations.contains(name))
          val enabled = enableAmr || enableMin
          if (enabled) {
            afterEvaluate {
              if (isCanBeResolved) {
                if (enableAmr) attributes.attribute(modularized, "yep")
                if (enableMin) attributes.attribute(minified, "yep")
              }
            }
          }
        }
      }
    }
  }
}

/** Configure transforms for resolved artifacts. */
internal fun Project.configureTransforms(conventions: ElideBuildExtension) {
  if (enableTransforms && conventions.deps.enableTransforms) {
    dependencies {
      registerTransform(AutomaticModuleTransform::class) {
        from.attribute(modularized, "nope").attribute(artifactType, "jar")
        to.attribute(modularized, "yep").attribute(artifactType, "jar")
      }
      registerTransform(JarMinifier::class) {
        from.attribute(minified, "nope").attribute(artifactType, "jar")
        to.attribute(minified, "yep").attribute(artifactType, "jar")
      }
    }
  }
}

/** Configures Java 9 modularity. */
internal fun Project.configureJavaModularity(moduleNameOverride: String? = null) {
  Java9Modularity.configure(this, moduleNameOverride)
}
