/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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
