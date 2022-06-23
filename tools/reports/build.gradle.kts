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
  testReportAggregation(project(":base"))
  testReportAggregation(project(":server"))
  testReportAggregation(project(":frontend"))
  testReportAggregation(project(":graalvm"))
  testReportAggregation(project(":graalvm-js"))
  testReportAggregation(project(":graalvm-react"))
  testReportAggregation(project(":rpc:js"))
  testReportAggregation(project(":rpc:jvm"))
}

tasks.create("reports") {
  dependsOn(tasks.named<TestReport>("testAggregateTestReport"))
}
