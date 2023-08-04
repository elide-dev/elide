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

package elide.server.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable

/**
 * Configuration loaded at runtime which governs Elide's built-in asset serving tools.
 */
@ConfigurationProperties("elide.server.assets")
public interface AssetConfig : Toggleable {
  public companion object {
    /** Default asset endpoint prefix. */
    public const val DEFAULT_ASSET_PREFIX: String = "/_/assets"

    /** Default enablement status for ETags support. */
    public const val DEFAULT_ENABLE_ETAGS: Boolean = true

    /** Whether to prefer weak ETag values when generating. */
    public const val DEFAULT_PREFER_WEAK_ETAGS: Boolean = false

    /** Whether to enable asset rewriting and optimization by default. */
    public const val DEFAULT_REWRITING_ENABLED: Boolean = true
  }

  /**
   * @return URI prefix where static assets are served.
   */
  public val prefix: String? get() = DEFAULT_ASSET_PREFIX

  /**
   * @return Whether to generate, and respond to, ETag headers for assets.
   */
  public val etags: Boolean? get() = DEFAULT_ENABLE_ETAGS

  /**
   * @return Whether to prefer weak ETags. Defaults to `false`.
   */
  public val preferWeakEtags: Boolean? get() = DEFAULT_PREFER_WEAK_ETAGS

  /**
   * @return Whether to enable or disable rewriting (globally -- all rewriting features).
   */
  public val rewriting: Boolean? get() = DEFAULT_REWRITING_ENABLED

  /**
   * @return Whether to rewrite asset links based on their content hashes.
   */
  public val hashLinks: Boolean? get() = DEFAULT_REWRITING_ENABLED
}
