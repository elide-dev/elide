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
@file:Suppress("MagicNumber")

package elide.internal.conventions.tests

import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.the
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import elide.internal.conventions.Constants
import elide.internal.conventions.isCI

/** Adjust settings related to test execution according to project conventions. */
internal fun Project.configureTestExecution() {
  tasks.withType(Test::class.java).configureEach {
    maxParallelForks = Constants.Tests.MAX_PARALLEL_FORKS
  }
}

/** Configure VM args and aspects of test running. */
internal fun Test.configureTestVm(toolchain: Int) {
  val defs = mutableListOf<Pair<String, String>>(
    "elide.test" to "true",
    "elide.internals" to "true",
    "truffle.TruffleRuntime" to "com.oracle.truffle.api.impl.DefaultTruffleRuntime",
  )
  val args = mutableListOf<String>(
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseG1GC",
    "-XX:+TrustFinalNonStaticFields",
    "-Xshare:auto",
  )
  if (toolchain > 21) {
    // add args for regular native access; enable unsupported modules (for unsafe)
    args.addAll(listOf(
      "--add-modules=jdk.unsupported",
      "--enable-native-access=ALL-UNNAMED",
    ))
  }
  if (toolchain > 23) {
    // add args for native access at jvm24+
    args.addAll(listOf(
      "--sun-misc-unsafe-memory-access=allow",
      "--illegal-native-access=allow",
    ))
  }

  defs.toMap().forEach {
    if (it.key !in systemProperties) {
      systemProperties[it.key] = it.value
    }
  }

  args.forEach {
    if (it !in jvmArgs) {
      jvmArgs.add(it)
    }
  }
}

/** Configure Kover test reports in CI. */
internal fun Project.configureKover() {
  extensions.getByType(KoverProjectExtension::class.java).apply {
    useJacoco(Constants.Versions.JACOCO)

    reports {
      filters {
        excludes {
          annotatedBy.addAll(
            "elide.annotations.Generated",
            "elide.annotations.engine.VMFeature",
            "com.oracle.svm.core.annotate.TargetClass",
          )
          inheritedFrom.addAll(
            "org.graalvm.nativeimage.hosted.Feature",
            "com.oracle.truffle.api.provider.InternalResourceProvider",
            "com.oracle.truffle.api.InternalResource\$Id",
          )
        }
      }
      total {
        xml {
          //  generate an XML report when running the `check` task in CI
          onCheck.set(isCI)
        }
      }
    }
  }

  // copy report to standard path for gradle, so that tooling can find it
  val koverXmlReport = tasks.named("koverXmlReport")
  val copyCoverageReport = tasks.register("copyCoverageReport", Copy::class.java) {
    dependsOn(koverXmlReport)
    from(project.layout.buildDirectory.file("reports/kover/report.xml"))
    into(project.layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
  }
  koverXmlReport.configure {
    finalizedBy(copyCoverageReport)
  }
}

/** Configure Jacoco test reports for JVM projects. */
internal fun Project.configureJacoco() {
  the<JacocoPluginExtension>().apply {
    toolVersion = Constants.Versions.JACOCO
  }
  tasks.named("jacocoTestReport", JacocoReport::class.java) {
    dependsOn(tasks.named("test"))
    reports.xml.required.set(true)
    classDirectories.setFrom(
      files(
        classDirectories.files.map {
          fileTree(it) {
            exclude(
              "**/generated/**",
              "**/com/**",
              "**/grpc/gateway/**",
              "**/tools/elide/**",
            )
          }
        },
      ),
    )
  }
}

/** Apply and configure the TestLogger plugin according to project conventions. */
internal fun Project.configureTestLogger() {
  // configure the extension
  extensions.getByType(TestLoggerExtension::class.java).apply {
    // use mocha-themed output
    theme = ThemeType.MOCHA_PARALLEL

    val testExceptions = isCI || System.getenv("TEST_EXCEPTIONS") != null
    val testVerbose = isCI || System.getenv("TEST_VERBOSE") != null
    val testLogs = System.getenv("TEST_LOGS") != null

    // only show errors if specified in the environment
    showExceptions = testExceptions

    // only show passed + skipped tests in verbose mode
    showPassed = testVerbose || testLogs
    showSkipped = testVerbose || testLogs

    showFailed = true
    showFailedStandardStreams = true
    showFullStackTraces = true
    showSimpleNames = true

    slowThreshold = Constants.Tests.SLOW_TEST_THRESHOLD
    showStandardStreams = testVerbose && testLogs
  }
}
