/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.runtime.core

import elide.annotations.Singleton
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.ALLOW_ALL
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.ALLOW_NONE

/**
 * This class acts as the root of the engine configuration DSL, allowing plugins to be
 * [installed][PluginRegistry.install] and exposing general features such as
 * [enabling support for specific languages][enableLanguage].
 *
 * Instances of this class cannot be created manually, instead, they are provided by the [PolyglotEngine] method, which
 * serves as entry point for the DSL.
 */
@DelicateElideApi @Singleton public abstract class PolyglotEngineConfiguration internal constructor() : PluginRegistry {
  /**
   * Enumerates the access privileges that can be conceded to guest code over host resources, such as environment
   * variables, or file system (IO).
   *
   * @see hostAccess
   */
  public enum class HostAccess {
    /** Allow guest code to access the host file system instead of an embedded, in-memory VFS. */
    ALLOW_IO,

    /** Allow guest code to access environment variables from the host. */
    ALLOW_ENV,

    /** Allow both filesystem and environment access. */
    ALLOW_ALL,

    /** Restrict all access to the host environment. */
    ALLOW_NONE,
  }
  
  /** The access granted to guest code over host resources, such as environment variables and the file system. */
  public var hostAccess: HostAccess = ALLOW_NONE

  /** Information about the platform hosting the runtime. */
  public val hostPlatform: HostPlatform = HostPlatform.resolve()

  /** Information about the runtime engine. */
  public abstract val hostRuntime: HostRuntime

  /** Enables support for the specified [language] on all contexts created by the engine. */
  public abstract fun enableLanguage(language: GuestLanguage)
}
