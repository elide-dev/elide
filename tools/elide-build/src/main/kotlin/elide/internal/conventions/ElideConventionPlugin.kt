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

package elide.internal.conventions

import com.adarshr.gradle.testlogger.TestLoggerPlugin
import com.diffplug.gradle.spotless.SpotlessPlugin
import com.github.benmanes.gradle.versions.VersionsPlugin
import io.gitlab.arturbosch.detekt.DetektPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.sonarqube.gradle.SonarQubePlugin
import javax.inject.Inject
import kotlinx.kover.gradle.plugin.KoverGradlePlugin
import elide.internal.conventions.Constants.Versions
import elide.internal.conventions.ElideBuildExtension.Convention
import elide.internal.conventions.archives.excludeDuplicateArchives
import elide.internal.conventions.archives.reproducibleArchiveTasks
import elide.internal.conventions.dependencies.configureDependencyLocking
import elide.internal.conventions.dependencies.configureDependencyResolution
import elide.internal.conventions.docs.DokkaConventionsPlugin
import elide.internal.conventions.jvm.*
import elide.internal.conventions.kotlin.KotlinTarget.JVM
import elide.internal.conventions.kotlin.KotlinTarget.WASM
import elide.internal.conventions.kotlin.configureKotlinBuild
import elide.internal.conventions.linting.*
import elide.internal.conventions.native.configureNativeBuild
import elide.internal.conventions.native.publishNativeLibrary
import elide.internal.conventions.project.configureProject
import elide.internal.conventions.publishing.configurePublishing
import elide.internal.conventions.publishing.configurePublishingRepositories
import elide.internal.conventions.publishing.configureSigning
import elide.internal.conventions.publishing.configureSigstore
import elide.internal.conventions.tests.configureJacoco
import elide.internal.conventions.tests.configureKover
import elide.internal.conventions.tests.configureTestExecution
import elide.internal.conventions.tests.configureTestLogger
import elide.internal.conventions.tests.configureTestVm

public abstract class ElideConventionPlugin : Plugin<Project> {
  @get:Inject protected abstract val javaToolchainService: JavaToolchainService

  override fun apply(target: Project): Unit = with(target) {
    // versioning
    plugins.apply(VersionsPlugin::class.java)

    // linters and static analyzers
    plugins.apply(SpotlessPlugin::class.java)
    plugins.apply(DetektPlugin::class.java)
    plugins.apply(SonarQubePlugin::class.java)
    plugins.apply(KoverGradlePlugin::class.java)

    // publishing and distribution
    plugins.apply(PublishingPlugin::class.java)
    plugins.apply(MavenPublishPlugin::class.java)
    plugins.apply(DistributionPlugin::class.java)
    plugins.apply(SigningPlugin::class.java)

    // testing
    plugins.apply(TestLoggerPlugin::class.java)
  }

  /** Applies conventions used by all projects. Migrated from the `dev.elide.build.*` script plugins. */
  internal fun applyElideConventions(project: Project, config: ElideBuildExtension.() -> Unit) = with(project) {
    // resolve extension
    val conventions = ElideBuildExtension(project).apply(config)
    project.extensions.add("elide", conventions)

    // apply baseline conventions
    configureProject()
    configureDependencyResolution(conventions)
    configureDependencyLocking(conventions)

    // configure custom attribute schema and defaults
    configureAttributeSchema(conventions)

    // install transforms
    configureTransforms(conventions)

    // configure pinned critical dependencies
    configurePinnedDependencies(conventions)

    // apply dependency security rules (verification, locking)
    configureDependencySecurity(conventions)

    // -- Conventions: Archives ---------------------------------------------------------------------------------------
    //
    maybeApplyConvention(conventions.archives) {
      if (reproducibleTasks) reproducibleArchiveTasks()
      if (excludeDuplicates) excludeDuplicateArchives()
    }

    // -- Conventions: Docs -------------------------------------------------------------------------------------------
    //
    maybeApplyConvention(conventions.docs) {
      if (conventions.docs.enabled) plugins.apply(DokkaConventionsPlugin::class.java)
    }

    // -- Conventions: Kotlin -----------------------------------------------------------------------------------------
    //
    maybeApplyConvention(conventions.kotlin) {
      // instead of defining JVM as the default value for the 'target' property, we need to declare it as nullable,
      // otherwise the default value's singleton breaks and appears as 'null' (no idea why).
      val kotlinTarget = target ?: JVM

      // automatically apply the JVM and Java conventions for Kotlin/JVM targets
      if (kotlinTarget is JVM) {
        conventions.jvm.requested = true
        conventions.java.requested = true
      }

      // apply jvm conventions when Kotlin/JVM is at least part of the compilation
      // this is required for proper JVM version alignment for example
      if (JVM in kotlinTarget) {
        conventions.jvm.requested = true

        // for Kotlin projects, these two features need to be configured separately,
        // since the java plugin is not always applied
        conventions.java.apply {
           includeJavadoc = false
           includeSources = false
        }
      }

      // if we're creating a WASM target, create a tree of source sets which can be used as commons.
      if (WASM in kotlinTarget || kotlinTarget is WASM) {
        conventions.kotlin.wasmSourceSets = true
      }

      // configure kotlin build
      configureKotlinBuild(
        target = kotlinTarget,
        conventions = conventions.kotlin,
        configureJavaModules = !project.isJpmsDisabled() && conventions.java.configureModularity,
        jvmModuleName = conventions.java.moduleName,
      )

      // kotlin linting tools
      plugins.apply(DetektConventionsPlugin::class.java)
      if (conventions.checks.sonar) plugins.apply(SonarConventionsPlugin::class.java)
      if (conventions.checks.spotless) plugins.apply(SpotlessConventionsPlugin::class.java)
    }

    // -- Conventions: Java -------------------------------------------------------------------------------------------
    //
    maybeApplyConvention(conventions.java) {
      configureJava()

      if (includeJavadoc && conventions.docs.enabled) includeJavadocJar()
      if (includeSources) includeSourceJar()

      if (!project.isJpmsDisabled() && configureModularity && !conventions.kotlin.requested) {
        configureJavaModularity(conventions.java.moduleName)
      }

      // java linting tools
      if (conventions.java.requested) {
        // pmd enablement
        if (conventions.checks.pmd) project.pluginManager.apply(PmdConventionsPlugin::class.java)

        // checkstyle enablement
        if (conventions.checks.checkstyle) project.pluginManager.apply(CheckstyleConventionsPlugin::class.java)

        // spotless enablement; added by kotlin, so doesn't need to be re-added unless kotlin is omitted.
        if (!conventions.kotlin.requested) project.pluginManager.apply(SpotlessConventionsPlugin::class.java)
      }
    }

    // -- Conventions: JVM --------------------------------------------------------------------------------------------
    //

    val defaultJvmTargetString = (findProperty(Versions.JVM_TARGET) as? String) ?: Versions.JVM_DEFAULT
    val defaultJvmTarget = JvmTarget.fromTarget(defaultJvmTargetString)
    var jvmTarget: JvmTarget = defaultJvmTarget

    val defaultJvmToolchainString = (findProperty(Versions.JVM_TOOLCHAIN) as? String) ?: Versions.JVM_TOOLCHAIN_DEFAULT
    var jvmToolchain: Int = defaultJvmToolchainString.toInt()

    maybeApplyConvention(conventions.jvm) {
      if (alignVersions) alignJvmVersion()
      if (forceJvm17) alignJvmVersion(overrideVersion = Versions.JVM_DEFAULT)
      else if (target != null) {
        jvmTarget = requireNotNull(target)
        alignJvmVersion(overrideVersion = target!!.target)
      }
    }

    // -- Conventions: Native -----------------------------------------------------------------------------------------
    //
    maybeApplyConvention(conventions.native) {
      if (publish) publishNativeLibrary()

      configureNativeBuild(
        target = this.target,
        enableAgent = useAgent,
        customLauncher = configureLauncher,
        toolchains = javaToolchainService,
      )
    }

    // -- Conventions: Publishing -------------------------------------------------------------------------------------
    //
    maybeApplyConvention(conventions.publishing) {
      configurePublishing(id, name, description)
      val doSigning = (findProperty(Constants.Publishing.ENABLE_SIGNING) as? String)?.toBooleanStrictOrNull()
        ?: signPublications
      val doSigstore = (findProperty(Constants.Publishing.ENABLE_SIGSTORE) as? String)?.toBooleanStrictOrNull()
        ?: enableSigstore

      if (commonRepositories) configurePublishingRepositories()
      if (doSigning) configureSigning()
      if (doSigstore) configureSigstore()
    }

    // -- Conventions: Testing ----------------------------------------------------------------------------------------
    //
    maybeApplyConvention(conventions.testing) {
      configureTestExecution()
      configureTestLogger()

      if (conventions.kotlin.requested && kover) configureKover()
      if (conventions.jvm.requested && jacoco) configureJacoco()

      tasks.withType<Test>().configureEach {
        configureTestVm(jvmToolchain)
      }
    }
  }

  private inline fun <T : Convention> maybeApplyConvention(convention: T, block: T.() -> Unit) {
    // only apply if requested
    if (!convention.requested) return
    convention.apply(block)
  }

  private fun Project.isJpmsDisabled(): Boolean {
    return project.findProperty(JPMS_DISABLE_SWITCH)?.toString()?.toBooleanStrictOrNull() ?: false
  }

  private companion object {
    /**
     * A property used as a switch to forcibly disable processing of all JPMS declarations, bypassing per-project
     * settings and extension configuration.
     */
    private const val JPMS_DISABLE_SWITCH = "elide.build.jpms.disable"
  }
}
