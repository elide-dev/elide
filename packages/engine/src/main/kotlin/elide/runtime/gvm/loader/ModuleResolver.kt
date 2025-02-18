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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.loader

import elide.runtime.core.DelicateElideApi

/**
 * ## Module Resolver
 */
public interface ModuleResolver {
  /**
   * Resolve a module by request.
   */
  public operator fun contains(mod: ModuleInfo): Boolean

  /**
   * Load a module from its [ModuleInfo].
   *
   * @param info The module info.
   * @return The module implementation; expected to be a polyglot value or proxy-type object.
   */
  public fun load(info: ModuleInfo): Any
}
