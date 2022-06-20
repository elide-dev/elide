
plugins {
  id("com.gradle.enterprise") version("3.10.2")
}

rootProject.name = "elide"

include(
  ":base",
  ":frontend",
  ":server",
)

val buildSamples: String by settings

if (buildSamples == "true") {
  include(
    ":samples:server:hellocss",
    ":samples:server:helloworld",
    ":samples:fullstack:basic",
  )
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}
