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

package elide.internal.conventions.jvm

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.the
import org.gradle.testing.base.TestingExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import elide.internal.conventions.Constants.Build
import elide.internal.conventions.Constants.Versions
import elide.internal.conventions.publishing.publishJavadocJar
import elide.internal.conventions.publishing.publishSourcesJar

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

/** Include the "sources" JAR in the Java compilation. */
internal fun Project.includeSourcesJar() {
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
      kotlinOptions {
        jvmTarget = targetJvmVersion
        javaParameters = true
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

/** Configures Java 9 modularity. */
internal fun Project.configureJavaModularity() {
  Java9Modularity.configure(this)
}
