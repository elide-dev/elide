plugins {
  id("com.gradle.enterprise") version("3.10.2")
}

rootProject.name = "elide"

include(
  ":packages:base",
  ":packages:frontend",
  ":packages:server",
  ":packages:graalvm",
  ":packages:graalvm-js",
  ":packages:graalvm-react",
  ":packages:model",
  ":packages:rpc:js",
  ":packages:rpc:jvm",
  ":packages:test",
  ":tools:reports",
)

val buildSamples: String by settings

if (buildSamples == "true") {
  include(
    ":samples:server:hellocss",
    ":samples:server:helloworld",
    ":samples:fullstack:basic:frontend",
    ":samples:fullstack:basic:server",
    ":samples:fullstack:react:frontend",
    ":samples:fullstack:react:server",
    ":samples:fullstack:ssr:node",
    ":samples:fullstack:ssr:server",
    ":samples:fullstack:react-ssr:frontend",
    ":samples:fullstack:react-ssr:node",
    ":samples:fullstack:react-ssr:server",
  )
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}
