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
        url = "https://us-docker.pkg.dev"
        username = "_json_key"
        password = file(creds).readText()
      }
    } else error(
      "Failed to resolve Docker credentials for CI"
    )
  }
}
