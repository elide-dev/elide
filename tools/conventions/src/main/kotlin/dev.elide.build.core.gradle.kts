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

import java.net.URI

plugins {
  `maven-publish`
  distribution
  signing
  idea

  id("dev.elide.build")
  id("com.adarshr.test-logger")
  id("com.github.ben-manes.versions")
  id("com.diffplug.spotless")
  id("io.gitlab.arturbosch.detekt")
  id("org.sonarqube")
  id("dev.sigstore.sign")
}

tasks.register("resolveAndLockAll") {
  doFirst {
    require(gradle.startParameter.isWriteDependencyLocks)
  }
  doLast {
    configurations.filter {
      // Add any custom filtering on the configurations to be resolved
      it.isCanBeResolved
    }.forEach { it.resolve() }
  }
}

// Artifacts: Publishing
// ---------------------
// Settings for publishing library artifacts to Maven repositories.
publishing {
  repositories {
    maven {
      name = "elide"
      url = URI.create(project.properties["elide.publish.repo.maven"] as String)

      if (project.hasProperty("elide.publish.repo.maven.auth") &&
          project.properties["elide.publish.repo.maven.auth"] == "true") {
        credentials {
          username = (project.properties["elide.publish.repo.maven.username"] as? String
            ?: System.getenv("PUBLISH_USER"))?.ifBlank { null }
          password = (project.properties["elide.publish.repo.maven.password"] as? String
            ?: System.getenv("PUBLISH_TOKEN"))?.ifBlank { null }
        }
      }
    }
  }
}

// Artifacts: Sigstore
// ------------------
// Publish artifact signature registrations to Sigstore.
sigstoreSign {
  oidcClient {
    gitHub {
      audience = "sigstore"
    }
    web {
      clientId = "sigstore"
      issuer = "https://oauth2.sigstore.dev/auth"
    }
    if (project.properties["elide.ci"] == "true") {
      client = gitHub
    } else {
      client = web
    }
  }
}

// Artifacts: Signing
// ------------------
// If so directed, make sure to sign outgoing artifacts.
signing {
  if (project.hasProperty("enableSigning") && project.properties["enableSigning"] == "true") {
    sign(configurations.archives.get())
    sign(publishing.publications)
  }
}
