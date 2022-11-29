plugins {
  kotlin("jvm")
  id("io.micronaut.docker")
}

// Compiler: Docker
// ----------------
// Configure Docker compiler.
docker {
  if (project.hasProperty("elide.ci") && (project.properties["elide.ci"] as String) == "true") {
    val creds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if (creds?.isNotBlank() == true) {
      registryCredentials {
        url.set("https://us-docker.pkg.dev")
        username.set("_json_key")
        password.set(file(creds).readText())
      }
    } else error(
      "Failed to resolve Docker credentials for CI"
    )
  }
}
