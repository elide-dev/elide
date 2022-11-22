@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

plugins {
  `java-library`
  publishing

  kotlin("plugin.serialization")
  id("dev.elide.build.jvm.kapt")
}

group = "dev.elide"
version = rootProject.version as String

dependencies {
  implementation(platform(project(":packages:platform")))
  implementation(project(":packages:base"))
  implementation(project(":packages:server"))
  implementation(libs.picocli)
  implementation(libs.micronaut.picocli)
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
    }
  }
}

sonarqube {
  isSkipProject = true
}
