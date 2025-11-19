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

@file:Suppress(
  "UnstableApiUsage",
  "unused",
)

plugins {
  `test-report-aggregation`
  `jacoco-report-aggregation`

  id(libs.plugins.sonar.get().pluginId)
  id(libs.plugins.kover.get().pluginId)
  alias(libs.plugins.elide.conventions)
}

val antJUnit: Configuration by configurations.creating

kover {
  disable()
}

val testAggregateTestReport by reporting.reports.creating(AggregateTestReport::class) {
  // testType = TestSuiteType.UNIT_TEST
}

val testCodeCoverageReport by reporting.reports.creating(JacocoCoverageReport::class) {
  // testType = TestSuiteType.UNIT_TEST
  reportTask {
    reports {
      xml.required = true
    }
  }
}

dependencies {
  antJUnit("org.apache.ant:ant-junit:1.10.12")
}

val locateCopyJUnitReports: TaskProvider<Copy> by tasks.registering(Copy::class) {
  val testReportPaths: List<File> = rootProject.allprojects.filter { project ->
    !listOf(
      "proto",
      "sample",
      "docs",
      "benchmarks",
    ).any {
      project.path.contains(it) || project.name.contains(it)
    }
  }.map {
    listOfNotNull(
      it.tasks.findByName("test"),
      it.tasks.findByName("jvmTest"),
    ).let { tasks ->
      dependsOn(tasks)
    }
    val path = file(it.layout.buildDirectory.dir("test-results"))
    if (path.exists()) {
      java.util.Optional.of(path)
    } else {
      java.util.Optional.empty()
    }
  }.filter {
    it.isPresent
  }.map {
    it.get()
  }

  testReportPaths.forEach {
    from(it) {
      include(
        "TEST-*.xml",
        "*/TEST-*.xml",
        "**/TEST-*.xml",
        "./*/TEST-*.xml",
        "./**/TEST-*.xml",
      )
    }
  }
  into(project.layout.buildDirectory.dir("test-results/allreports").get())
}

val resultsDir = file(layout.buildDirectory.dir("test-results/allreports"))
val mergedDir = file(layout.buildDirectory.dir("test-results"))

val copyFinalizedReports: TaskProvider<Copy> by tasks.registering(Copy::class) {
  dependsOn(locateCopyJUnitReports, mergeJUnitReports)
  mustRunAfter(locateCopyJUnitReports, mergeJUnitReports)

  from(mergedDir) {
    include("**/*.*")
  }
  into(rootProject.layout.buildDirectory.dir("test-results/merged"))
}

val mergeJUnitReports: TaskProvider<Task> by tasks.registering {
  dependsOn(locateCopyJUnitReports)
  finalizedBy(copyFinalizedReports)

  if (resultsDir.exists()) {
    doLast {
      ant.withGroovyBuilder {
        "taskdef"(
          "name" to "junitreport",
          "classname" to "org.apache.tools.ant.taskdefs.optional.junit.XMLResultAggregator",
          "classpath" to antJUnit.asPath
        )

        // generates an XML report
        "junitreport"("todir" to mergedDir) {
          "fileset"(
            "dir" to resultsDir,
            "includes" to "**/*.xml"
          )
        }
      }
    }
  }
}

val reports: TaskProvider<Task> by tasks.registering {
  description = "Generate all project reports"
  group = "reporting"

  dependsOn("testAggregateTestReport", mergeJUnitReports)
}
