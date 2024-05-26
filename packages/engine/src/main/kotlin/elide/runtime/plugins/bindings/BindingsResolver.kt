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
package elide.runtime.plugins.bindings

import elide.runtime.core.DelicateElideApi

/** A component that dynamically resolves language binding installers at configuration time. */
@DelicateElideApi public fun interface BindingsResolver {
  /** Resolve and return a sequence of applicable [BindingsInstaller] implementations. */
  public fun resolveBindings(): Sequence<BindingsInstaller>

  /** An empty resolver implementation which always returns an empty sequence. */
  public object Empty : BindingsResolver {
    override fun resolveBindings(): Sequence<BindingsInstaller> = emptySequence()
  }
}
