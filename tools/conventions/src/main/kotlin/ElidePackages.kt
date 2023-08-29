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

@file:Suppress("UnstableApiUsage")

import Java9Modularity.configure as configureJava9ModuleInfo
import com.google.devtools.ksp.gradle.KspTask
import dev.sigstore.sign.SigstoreSignExtension
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.jetbrains.kotlin.gradle.internal.KaptTask
import java.util.concurrent.atomic.AtomicBoolean

/**
 * # Package Tools
 *
 * Provides consistent package publishing logic.
 */
object ElidePackages {
  private val publicationTasks: List<String> = listOf(
    "publishToSonatype",
  )

  private val allDevelopers = listOf(
    PomDeveloper(id = "sgammon", name = "Sam Gammon", email = "samuel.gammon@gmail.com"),
    PomDeveloper(id = "darvld", name = "Dario Valdespino"),
  )

  data class PomDeveloper(
    var id: String,
    var name: String,
    var email: String? = null,
  )

  data class PomInfo(
    var id: String,
    var name: String,
    var description: String,
    var link: String = "https://elide.dev",
    var developers: MutableList<PomDeveloper>,
  )

  interface PackageContext {
    var java9Modularity: Boolean
    var enableSigstore: Boolean

    var id: String
      get() = pom.id
      set(value) {
        pom.id = value
      }

    var name: String
      get() = pom.name
      set(value) {
        pom.name = value
      }

    var description: String
      get() = pom.description
      set(value) {
        pom.description = value
      }

    fun developer(id: String, name: String, email: String? = null) {
      pom.developers.add(PomDeveloper(id, name, email))
    }

    val pom: PomInfo
  }

  fun elidePomDefaults(
    id: String,
    name: String,
    description: String,
  ): PomInfo = PomInfo(
    id = id,
    name = name,
    description = description,
    developers = allDevelopers.toMutableList(),
  )

  fun Project.publishable(id: String, name: String, description: String, javadocJarTask: TaskProvider<Jar>? = null) {
    the<PublishingExtension>().publishable(
      this,
      javadocJarTask = javadocJarTask,
      info = elidePomDefaults(
        id = id,
        name = name,
        description = description,
      ),
    )
  }

  fun PublishingExtension.publishable(project: Project, info: PomInfo, javadocJarTask: TaskProvider<Jar>? = null) {
    publications.withType<MavenPublication> {
      if (
        project.properties["buildDocs"] != "false" &&
        project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
      ) {
        (javadocJarTask ?: project.tasks.named("javadocJar").orNull)?.let { task ->
          artifact(task)
        }
      }

      // prepare artifact info
      artifactId = artifactId.replace(info.id, "elide-${info.id}")

      pom {
        name.set(info.name)
        url.set(info.link)
        description.set(info.description)

        licenses {
          license {
            name.set("MIT License")
            url.set("https://github.com/elide-dev/elide/blob/v3/LICENSE")
          }
        }
        info.developers.map {
          developers {
            developer {
              id.set(it.id)
              name.set(it.name)
              if (it.email != null) {
                email.set(it.email)
              }
            }
          }
        }
        scm {
          url.set("https://github.com/elide-dev/elide")
        }
      }
    }
  }

  private fun Project.configureDocs(buildDocs: Boolean): TaskProvider<Jar>? {
    return if (buildDocs) {
      if (project.plugins.hasPlugin("org.jetbrains.kotlin.kapt")) {
        project.tasks.named("dokkaHtml").orNull?.dependsOn(
          project.tasks.withType(KaptTask::class),
        )
        listOf("dokkaJavadoc").forEach {
          tasks.findByName(it)?.apply {
            dependsOn("kaptKotlin")
          }
        }
      }
      if (project.plugins.hasPlugin("com.google.devtools.ksp")) {
        project.tasks.named("dokkaHtml").orNull?.dependsOn(
          project.tasks.withType(KspTask::class)
        )
      }
      val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

      when (tasks.findByName("javadocJar")) {
        null -> {
          val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
            dependsOn(dokkaHtml)
            archiveClassifier = "javadoc"
            from(dokkaHtml.outputDirectory)
          }
          javadocJar
        }
        else -> tasks.named("javadocJar", Jar::class).apply {
          configure {
            dependsOn(dokkaHtml)
            archiveClassifier = "javadoc"
            from(dokkaHtml.outputDirectory)
          }
        }
      }
    } else null
  }

  private fun Project.configureSigstore() {
    apply(plugin = "dev.sigstore.sign")

    the<SigstoreSignExtension>().apply {
      oidcClient {
        gitHub {
          audience.set("sigstore")
        }
        web {
          clientId.set("sigstore")
          issuer.set("https://oauth2.sigstore.dev/auth")
        }
      }
    }
  }

  private fun Project.configureSigning(): TaskCollection<Sign> {
    val signingTasks = project.tasks.withType(Sign::class)

    listOf(
      AbstractPublishToMaven::class,
    ).forEach {
      project.tasks.withType(it).configureEach {
        dependsOn(signingTasks)
      }
    }
    return signingTasks
  }

  fun Project.elidePackage(pkg: PackageContext) {
    val buildDocs = properties["buildDocs"] != "false"
    val enableSigning = properties["enableSigning"] != "false"
    val release = properties["elide.release"] == "true" || properties["elide.buildMode"] == "release"
    val enableSigstore = pkg.enableSigstore && properties["enableSigstore"] != "false"

    // docs typically depend on the output of kapt or ksp
    val javadocJar = if (!buildDocs && release) {
      error("Cannot release library packages without docs. Please pass `buildDocs=true`.")
    } else {
      configureDocs(buildDocs)
    }

    // central publishing requires signing
    if (enableSigning) {
      if (enableSigstore) configureSigstore()
      configureSigning()
    } else if (release) {
      error("Cannot release library packages without signing. Please pass `enableSigning=true`.")
    }

    project.tasks.register("publishAllElidePublications") {
      group = "Publishing"
      description = "Publish all release publications for this Elide package"
      publicationTasks.map(project.tasks::named).map {
        dependsOn(it)
      }
    }

    // configure java9 modularity and multi-jvm classes
    if (pkg.java9Modularity) configureJava9ModuleInfo(project)

    tasks.withType(Jar::class) {
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    // packages are publishable
    publishable(pkg.id, pkg.name, pkg.description, javadocJarTask = javadocJar)
  }

  fun Project.elidePackage(
    id: String,
    name: String,
    description: String,
  ) {
    elidePackage(id, name, description) {
      // nothing
    }
  }

  fun Project.elidePackage(id: String, name: String, description: String, configurator: PackageContext.() -> Unit) {
    val pomInfo = elidePomDefaults(id, name, description)
    val enableJLink = AtomicBoolean(
      project.plugins.hasPlugin("java") ||
      project.plugins.hasPlugin("org.jetbrains.kotlin.jvm") ||
      project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") && project.tasks.findByName("jvmJar") != null
    )
    val enableSigstore = AtomicBoolean(true)

    val ctx = object : PackageContext {
      override var java9Modularity: Boolean
        get() = enableJLink.get()
        set(value) { enableJLink.set(value) }

      override var enableSigstore: Boolean
        get() = enableSigstore.get()
        set(value) { enableSigstore.set(value) }

      override val pom: PomInfo get() = pomInfo
    }.apply {
      configurator.invoke(this)
    }

    elidePackage(ctx)
  }
}
