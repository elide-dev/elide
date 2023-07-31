@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("project-report")
  id("test-report-aggregation")
  id("jacoco-report-aggregation")
  id("org.sonarqube")
  id("org.jetbrains.kotlinx.kover")
}

val antJUnit: Configuration by configurations.creating

kover {
  isDisabled = true
}

reporting {
  reports {
    val testAggregateTestReport by creating(AggregateTestReport::class) {
      testType = TestSuiteType.UNIT_TEST
    }
    val testCodeCoverageReport by creating(JacocoCoverageReport::class) {
      testType = TestSuiteType.UNIT_TEST
      reportTask {
        reports {
          xml.required = true
        }
      }
    }
  }
}

val buildSsg by properties

dependencies {
  Elide.serverModules.plus(
    if (buildSsg == "true") {
      listOf("ssg")
    } else {
      emptyList()
    }
  ).plus(
    Elide.multiplatformModules
  ).forEach {
    testReportAggregation(project(":packages:$it"))
    jacocoAggregation(project(":packages:$it"))
  }

  antJUnit("org.apache.ant", "ant-junit", Versions.antJUnit)
}

task<Copy>("locateCopyJUnitReports") {
  val testReportPaths: List<String> = rootProject.allprojects.filter { project ->
    !listOf(
      "proto",
      "sample",
      "docs",
      "benchmarks",
    ).any {
      project.path.contains(it) || project.name.contains(it)
    }
  }.map {
    val path = file(layout.buildDirectory.dir("test-results/test"))
    if (path.exists()) {
      java.util.Optional.of(path)
    } else {
      java.util.Optional.empty()
    }
  }.filter {
    it.isPresent
  }.map {
    it.get().absolutePath
  }

  testReportPaths.forEach {
    from(it) {
      include("TEST-*.xml")
      include("**/TEST-*.xml")
    }
  }
  into(
    "build/test-results/allreports"
  )
}

task("mergeJUnitReports") {
  dependsOn(tasks.named("locateCopyJUnitReports"))
  val resultsDir = file("build/test-results/allreports")
  val mergedDir = file("build/test-results")

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
            "includes" to "TEST-*.xml"
          )
        }
      }
    }
  }
}

tasks.create("reports") {
  dependsOn(tasks.named<TestReport>("testAggregateTestReport"))
  dependsOn(tasks.named("mergeJUnitReports"))

  // @TODO(sgammon): broken by proto module
  // dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
