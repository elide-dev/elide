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

package elide.tooling.jvm.resolver

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.transport.wagon.WagonTransporterFactory
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import kotlin.test.Test

@Suppress("DEPRECATION")
class ResolverTest {
  @Test fun testPlainResolverEndToEnd() {
    // Create a new Maven repository system
    val locator = MavenRepositorySystemUtils.newServiceLocator()
    locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
    locator.addService(TransporterFactory::class.java, WagonTransporterFactory::class.java)
    locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)

    val repoSystem = locator.getService(RepositorySystem::class.java)

    // Create a session for managing repository interactions
    val session = MavenRepositorySystemUtils.newSession()

    // Set local repository
    val localRepo = LocalRepository("build/test-target/local-repo")
    session.localRepositoryManager = repoSystem.newLocalRepositoryManager(session, localRepo)

    // Define remote repositories
    val mavenCentral = RemoteRepository.Builder(
      "central",
      "default",
      "https://repo1.maven.org/maven2/"
    ).build()

    // Define the artifact to resolve
    val artifact = DefaultArtifact("com.google.guava:guava:31.1-jre")
    val dependency = Dependency(artifact, null)

    // Create collect request
    val collectRequest = CollectRequest()
    collectRequest.root = dependency
    collectRequest.addRepository(mavenCentral)

    // Resolve dependencies
    val dependencyRequest = DependencyRequest(collectRequest, null)
    val dependencyResult = repoSystem.resolveDependencies(session, dependencyRequest)

    // Print resolved dependencies
    val nlg = PreorderNodeListGenerator()
    dependencyResult.root.accept(nlg)

    println("Resolved dependencies:")
    println(nlg.getDependencies(true).joinToString("\n"))
    println("\nClasspath:")
    println(nlg.classPath)
  }
}
