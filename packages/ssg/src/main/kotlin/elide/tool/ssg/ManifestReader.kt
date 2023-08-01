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

package elide.tool.ssg

import kotlinx.coroutines.Deferred
import tools.elide.meta.AppManifest
import java.io.Closeable

/**
 * # SSG: Manifest Reader
 *
 * This interface defines the API surface of implementations responsible for loading an Elide application manifest
 * during execution of the SSG (Static Site Generator) compiler.
 *
 * ## Filesystem & Classpath Resources
 *
 * By default, the manifest reader is expected to interpret given paths as filesystem paths, unless a given path is
 * prefixed with `classpath:`. In this case, the path is interpreted as a classpath resource.
 */
public interface ManifestReader : Closeable, AutoCloseable {
  /**
   * Read an application manifest, given the provided [params].
   *
   * @param params Compiler params to read a manifest from.
   * @return Application manifest read from the given parameters.
   * @throws SSGCompilerError the manifest cannot be located based on the input parameters.
   */
  @Throws(SSGCompilerError::class)
  public suspend fun readManifest(params: SiteCompilerParams): AppManifest = readManifest(params.manifest)

  /**
   * Read an application manifest, given the provided filesystem [path].
   *
   * @param path File-system path to the manifest.
   * @return Application manifest read from the given parameters.
   * @throws SSGCompilerError the manifest cannot be located based on the input parameters.
   */
  @Throws(SSGCompilerError::class)
  public suspend fun readManifest(path: String): AppManifest = readManifestAsync(path).await()

  /**
   * Read an application manifest asynchronously, given the provided filesystem [path].
   *
   * If a manifest cannot be located, an exception is raised after calling `await` on the returned job.
   *
   * @param path File-system path to the manifest.
   * @return Deferred job which resolved to the application manifest read from the given parameters.
   */
  public suspend fun readManifestAsync(path: String): Deferred<AppManifest>
}
