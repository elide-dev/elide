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
 * ## Module Factory
 *
 * A factory which creates an instance of a synthesized module; loaded from a [ModuleResolver].
 */
public fun interface ModuleFactory {
  /**
   * Load a module from a [ModuleInfo].
   *
   * @param module The module to load.
   * @return The loaded module.
   */
  public fun load(module: ModuleInfo): Any
}
