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
package dev.elide.intellij.psi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import fleet.multiplatform.shims.ConcurrentHashMap
import org.pkl.intellij.PklFileType
import org.pkl.intellij.packages.dto.PklProject
import org.pkl.intellij.psi.PklModuleResolverExtension
import java.net.URI
import elide.tooling.project.codecs.ElidePackageManifestCodec

class PklElideModuleResolver : PklModuleResolverExtension {
  private val moduleCache = ConcurrentHashMap<String, VirtualFile>()

  override fun resolveModuleFile(uri: String, project: Project, context: PklProject?): VirtualFile? {
    if (!uri.startsWith(ELIDE_URI_PREFIX)) return null

    return runCatching {
      moduleCache.getOrPut(uri) {
        val source = ElidePackageManifestCodec.ElideBuiltinModuleKey(URI.create(uri)).loadSource()
        LightVirtualFile(uri, PklFileType, source)
      }
    }.onFailure {
      LOG.warn("Failed to resolve $uri", it)
    }.getOrNull()
  }

  private companion object {
    private const val ELIDE_URI_PREFIX = "elide:"

    private val LOG = Logger.getInstance(PklElideModuleResolver::class.java)
  }
}
