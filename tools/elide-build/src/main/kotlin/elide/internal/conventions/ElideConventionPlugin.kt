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
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.plugins.signing.SigningPlugin
import org.sonarqube.gradle.SonarQubePlugin
import javax.inject.Inject
import elide.internal.conventions.ElideBuildExtension.Convention
import elide.internal.conventions.archives.excludeDuplicateArchives
import elide.internal.conventions.archives.reproducibleArchiveTasks
import elide.internal.conventions.dependencies.configureDependencyLocking
import elide.internal.conventions.dependencies.configureDependencyResolution
import elide.internal.conventions.docker.useGoogleCredentialsForDocker
import elide.internal.conventions.jvm.*
import elide.internal.conventions.kotlin.KotlinTarget.JVM
import elide.internal.conventions.kotlin.configureKotlinBuild
import elide.internal.conventions.native.configureNativeBuild
import elide.internal.conventions.native.publishNativeLibrary
import elide.internal.conventions.project.configureProject
import elide.internal.conventions.publishing.configurePublishing
import elide.internal.conventions.publishing.configurePublishingRepositories
import elide.internal.conventions.publishing.configureSigning
import elide.internal.conventions.publishing.configureSigstore
import elide.internal.conventions.tests.configureJacoco
import elide.internal.conventions.tests.configureKoverCI
import elide.internal.conventions.tests.configureTestExecution
import elide.internal.conventions.tests.configureTestLogger

public fun Project.elide(block: ElideBuildExtension.() -> Unit) {
  plugins.getPlugin(ElideConventionPlugin::class.java).applyElideConventions(this, block)
}

public abstract class ElideConventionPlugin : Plugin<Project> {
  @get:Inject protected abstract val javaToolchainService: JavaToolchainService

  override fun apply(target: Project): Unit = with(target) {
    // versioning
    plugins.apply(VersionsPlugin::class.java)

    // linters and static analyzers
    plugins.apply(SpotlessPlugin::class.java)
    plugins.apply(DetektPlugin::class.java)
    plugins.apply(SonarQubePlugin::class.java)

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

    // apply conventions
    configureProject()

    if (conventions.lockDependencies) configureDependencyLocking()
    if (conventions.strictDependencies) configureDependencyResolution()

    maybeApplyConvention(conventions.archives) {
      if (reproducibleTasks) reproducibleArchiveTasks()
      if (excludeDuplicates) excludeDuplicateArchives()
    }

    maybeApplyConvention(conventions.docker) {
      if (useGoogleCredentials) useGoogleCredentialsForDocker()
    }

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
      }

      configureKotlinBuild(
        target = kotlinTarget,
        configureKapt = kapt,
        configureAllOpen = allOpen,
        configureNoArgs = noArgs,
        explicitApi = explicitApi,
        configureJavaModules = conventions.java.configureModularity
      )
    }

    maybeApplyConvention(conventions.java) {
      configureJava()

      if (includeJavadoc) includeJavadocJar()
      if (includeSources) includeSourcesJar()
      if (configureModularity) configureJavaModularity()
    }

    maybeApplyConvention(conventions.jvm) {
      if (alignVersions) alignJvmVersion()
      if (forceJvm17) alignJvmVersion(overrideVersion = "17")
    }

    maybeApplyConvention(conventions.native) {
      if (publish) publishNativeLibrary()

      configureNativeBuild(
        target = this.target,
        enableAgent = useAgent,
        toolchains = javaToolchainService,
      )
    }

    maybeApplyConvention(conventions.publishing) {
      configurePublishing(id, name, description)

      if (commonRepositories) configurePublishingRepositories()
      if (signPublications) configureSigning()
      if (enableSigstore) configureSigstore()
    }

    maybeApplyConvention(conventions.testing) {
      configureTestExecution()
      configureTestLogger()

      if (kover) configureKoverCI()
      if (jacoco) configureJacoco()
    }
  }

  private inline fun <T : Convention> maybeApplyConvention(convention: T, block: T.() -> Unit) {
    // only apply if requested
    if (!convention.requested) return
    convention.apply(block)
  }
}