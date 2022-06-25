@file:Suppress("UnstableApiUsage", "UNUSED_VARIABLE")

plugins {
  id("test-report-aggregation")
  id("org.sonarqube")
}

reporting {
  reports {
    val testAggregateTestReport by creating(AggregateTestReport::class) {
      testType.set(TestSuiteType.UNIT_TEST)
    }
  }
}

dependencies {
  testReportAggregation(project(":packages:base"))
  testReportAggregation(project(":packages:server"))
  testReportAggregation(project(":packages:frontend"))
  testReportAggregation(project(":packages:graalvm"))
  testReportAggregation(project(":packages:graalvm-js"))
  testReportAggregation(project(":packages:graalvm-react"))
  testReportAggregation(project(":packages:rpc:js"))
  testReportAggregation(project(":packages:rpc:jvm"))
}

tasks.create("reports") {
  dependsOn(tasks.named<TestReport>("testAggregateTestReport"))
}
