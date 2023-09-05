package elide.internal.conventions.docker

import com.bmuschko.gradle.docker.DockerExtension
import org.gradle.api.Project
import elide.internal.conventions.Constants
import elide.internal.conventions.isCI

/** Configure the Docker extension in CI and load repository credentials. */
internal fun Project.useGoogleCredentialsForDocker() {
  // only run in CI
  if(!isCI) return
  
  // read credentials
  val credentials = System.getenv(Constants.Credentials.GOOGLE).let {
    check(it.isNotBlank()) { "Failed to resolve Docker credentials for CI" }
    file(it).readText()
  }
  
  // configure Docker
  extensions.getByType(DockerExtension::class.java).apply {
    registryCredentials.apply {
      url.set(Constants.Repositories.PKG_DOCKER)
      
      username.set("_json_key")
      password.set(credentials)
    }
  }
}