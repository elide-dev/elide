package elide.internal.conventions.jvm

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.testing.base.TestingExtension
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
    // TODO(@darvld): does this work?
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

  extensions.getByType(JavaPluginExtension::class.java).apply {
    sourceCompatibility = JavaVersion.toVersion(targetJvmVersion)
    targetCompatibility = JavaVersion.toVersion(targetJvmVersion)
  }

  tasks.withType(JavaCompile::class.java).configureEach {
    sourceCompatibility = targetJvmVersion
    targetCompatibility = targetJvmVersion

    options.isFork = true
    options.isIncremental = true
  }

  tasks.withType(KotlinCompile::class.java).configureEach {
    incremental = true
    kotlinOptions {
      jvmTarget = targetJvmVersion
      javaParameters = true
    }
  }
}

/** Registers the Javadoc JAR task. */
internal fun Project.configureJavaDoc() {
  val buildDocs = findProperty(Build.BUILD_DOCS)?.toString()?.toBoolean() ?: true

  tasks.create("javadocJar", Jar::class.java) {
    archiveClassifier.set("javadoc")

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    if (buildDocs) {
      from(tasks.named("dokkaJavadoc"))
    }
  }

  tasks.withType(Javadoc::class.java).configureEach {
    isFailOnError = false
  }
}

/** Configures Java 9 modularity. */
internal fun Project.configureJavaModularity() {
  Java9Modularity.configure(this)
}