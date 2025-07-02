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

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.spi.locator.Service
import org.eclipse.aether.spi.locator.ServiceLocator
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Named("gmm") @Singleton internal class GmmAwareArtifactDescriptorReader @Inject constructor (
  private var project: AetherProjectProvider? = null,
) : DefaultArtifactDescriptorReader(), Service {
  constructor(): this(null)

  override fun initService(locator: ServiceLocator) {
    project = locator.getService(AetherProjectProvider::class.java)
    super.initService(locator)
  }

  override fun readArtifactDescriptor(
    session: RepositorySystemSession,
    request: ArtifactDescriptorRequest
  ): ArtifactDescriptorResult {
    // for now, just proxy to our delegate @TODO
    return super.readArtifactDescriptor(session, request)
  }
}
