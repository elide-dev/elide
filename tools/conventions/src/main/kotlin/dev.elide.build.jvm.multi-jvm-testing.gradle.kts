@file:Suppress("UnstableApiUsage")

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.language.base.plugins.LifecycleBasePlugin
import kotlin.math.max
import kotlin.math.min

plugins {
  java
}

val defaultJavaMin = "11"
val defaultJavaMax = "19"

val os: OperatingSystem = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
  .getCurrentOperatingSystem()

val javaSdk: Int = (if (project.hasProperty("versions.java.language")) {
  project.properties["versions.java.language"] as? String ?: defaultJavaMin
} else {
  defaultJavaMin
}).toInt()

val baseJavaMin: Int = (if (project.hasProperty("versions.java.minimum")) {
  project.properties["versions.java.minimum"] as? String ?: defaultJavaMin
} else {
  defaultJavaMin
}).toInt()

val proposedJavaMin: Int = if (System.getProperty("os.arch") == "aarch64" && os.isMacOsX()) {
  // artificially start at java 17 for aarch64, which is the first version that supports this architecture.
  17
} else {
  baseJavaMin
}

val proposedJavaMax: Int = (if (project.hasProperty("versions.java.maximum")) {
  project.properties["versions.java.maximum"] as? String ?: defaultJavaMax
} else {
  defaultJavaMax
}).toInt()

// Minimum supported JVM version.
val javaMin = min(proposedJavaMin, proposedJavaMax)

// Maximum supported JVM version.
val javaMax = max(proposedJavaMin, proposedJavaMax)

/**
 * Enable Multi-JVM testing for the specified minimum/maximum range.
 *
 * @param min Proposed minimum JVM version.
 * @param max Proposed maximum JVM version.
 * @param vendor Desired vendor. If not specified, a sensible default is used (GraalVM).
 * @param engine Desired JVM engine. If unspecified, none is declared.
 */
fun Project.enableMultiJvmTesting(
  min: Int = javaMin,
  max: Int = javaMax,
  vendor: JvmVendorSpec = JvmVendorSpec.GRAAL_VM,
  engine: JvmImplementation? = null,
) {
  // Normal test task runs on compile JDK.
  (min..max).forEach { major ->
    val jdkTest = tasks.register("testJdk$major", Test::class.java) {
      description = "Runs the test suite on JDK $major"
      group = LifecycleBasePlugin.VERIFICATION_GROUP
      javaLauncher = javaToolchains.launcherFor {
        this.languageVersion = JavaLanguageVersion.of(major)
        this.vendor = vendor
        if (engine != null) {
          this.implementation = engine
        }
      }
      val testTask = tasks.named("test", Test::class.java).get()
      classpath = testTask.classpath
      testClassesDirs = testTask.testClassesDirs
    }
    val checkTask = tasks.named("check")
    checkTask.configure {
      dependsOn(jdkTest)
    }
  }
}

if (project.properties["elide.test.multi-jvm"] != "false") {
  enableMultiJvmTesting()
}
