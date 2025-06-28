@file:Suppress("DEPRECATION")

package elide.tooling.codecs.maven

import elide.runtime.Logging
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
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

@Factory
internal class RepositorySystemFactory {
  private val logging by lazy { Logging.Companion.of(RepositorySystem::class) }

  @Singleton
  fun mavenLocator(): DefaultServiceLocator {
    val mvnLocator = MavenRepositorySystemUtils.newServiceLocator()
    mvnLocator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    mvnLocator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
    return mvnLocator
  }

  @Singleton
  fun repositorySystem(locator: DefaultServiceLocator): DefaultRepositorySystem {
    return requireNotNull(locator.getService(RepositorySystem::class.java) as DefaultRepositorySystem) {
      "Failed to initialize Maven repository system: Repository system is null"
    }
  }

  @Singleton
  fun repositorySystemSession(): DefaultRepositorySystemSession {
    // Create a session for managing repository interactions
    logging.trace { "Creating repo session" }
    val repoSession = requireNotNull(MavenRepositorySystemUtils.newSession()) {
      "Failed to initialize Maven repository system: Repository session is null"
    }

    repoSession.cache = DefaultRepositoryCache()
    repoSession.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
    repoSession.updatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY
    return repoSession
  }

  @Singleton
  fun remoteRepositoryManager(locator: DefaultServiceLocator): RemoteRepositoryManager {
    return locator.getService(RemoteRepositoryManager::class.java)
  }
}
