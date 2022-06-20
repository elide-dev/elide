
plugins {
  id("com.gradle.enterprise") version("3.10.2")
}

rootProject.name = "elide"

include(
  ":base",
  ":frontend",
  ":server",
  ":samples:server:hellocss",
  ":samples:server:helloworld",
)

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}
