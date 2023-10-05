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

package elide.internal.conventions.tests

import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.TestLoggerPlugin
import com.adarshr.gradle.testlogger.theme.ThemeType
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import kotlinx.kover.gradle.plugin.dsl.KoverReportExtension
import elide.internal.conventions.Constants
import elide.internal.conventions.isCI

/** Adjust settings related to test execution according to project conventions. */
internal fun Project.configureTestExecution() {
  tasks.withType(Test::class.java).configureEach {
    maxParallelForks = Constants.Tests.MAX_PARALLEL_FORKS
  }
}

/** Configure Kover test reports in CI. */
internal fun Project.configureKoverCI() {
  extensions.getByType(KoverReportExtension::class.java).apply {
    defaults {
      //  generate an XML report when running the `check` task in CI
      xml { onCheck = isCI }
    }
  }
}

/** Configure Jacoco test reports for JVM projects. */
internal fun Project.configureJacoco() {
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

    // only show errors if specified in the environment
    showExceptions = System.getenv("TEST_EXCEPTIONS") == "true"

    showFailed = true
    showPassed = true
    showSkipped = true
    showFailedStandardStreams = true
    showFullStackTraces = true

    slowThreshold = Constants.Tests.SLOW_TEST_THRESHOLD
  }
}
