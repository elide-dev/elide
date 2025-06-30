/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
@file:Suppress("DEPRECATION")

package elide.tooling.jvm.resolver

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositoryCache
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.impl.RemoteRepositoryManager
import org.eclipse.aether.internal.impl.DefaultRepositorySystem
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.transport.wagon.WagonTransporterFactory
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.Logging

@Factory public class RepositorySystemFactory {
  private val logging by lazy { Logging.of(RepositorySystem::class) }

  @Singleton public fun mavenLocator(): DefaultServiceLocator {
    logging.debug { "Initializing Maven resolver" }
    val mvnLocator = MavenRepositorySystemUtils.newServiceLocator()
    mvnLocator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    mvnLocator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
    mvnLocator.addService(TransporterFactory::class.java, WagonTransporterFactory::class.java)
    mvnLocator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
    return mvnLocator
  }

  @Singleton public fun repositorySystem(locator: DefaultServiceLocator): DefaultRepositorySystem {
    // Create a new Maven repository system
    logging.trace { "Creating repo system" }
    return requireNotNull(locator.getService(RepositorySystem::class.java) as DefaultRepositorySystem) {
      "Failed to initialize Maven repository system: Repository system is null"
    }
  }

  @Singleton public fun repositorySystemSession(): DefaultRepositorySystemSession {
    // Create a session for managing repository interactions
    logging.trace { "Creating repo session" }
    val repoSession = requireNotNull(MavenRepositorySystemUtils.newSession()) {
      "Failed to initialize Maven repository system: Repository session is null"
    }

    // prepare repository cache
    val repoCache = DefaultRepositoryCache()
    repoSession.cache = repoCache
    repoSession.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
    repoSession.updatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY
    return repoSession
  }

  @Singleton public fun remoteRepositoryManager(locator: DefaultServiceLocator): RemoteRepositoryManager {
    return locator.getService(RemoteRepositoryManager::class.java)
  }
}
