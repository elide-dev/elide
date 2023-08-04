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

@file:Suppress("RedundantVisibilityModifier")

package elide.proto.api

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import elide.proto.ElideProtocol

/**
 * TBD.
 */
public object Protocol : ServiceLoader.Provider<ElideProtocol> {
  // Active default protocol implementation.
  private lateinit var activeDefaultProtocol: ElideProtocol

  // Active implementation class.
  private lateinit var activeImplType: Class<out ElideProtocol>

  // All installed protocol implementations.
  private val allProtocols: MutableList<ServiceLoader.Provider<out ElideProtocol>> = ArrayList()

  // Cached access to each instantiated protocol implementation.
  private val implMap: TreeMap<ElideProtocol.ImplementationLibrary, ElideProtocol> = TreeMap()

  // Whether the default protocol has loaded.
  private val initialized = AtomicBoolean(false)

  /**
   * TBD.
   */
  public fun acquire(impl: ElideProtocol.ImplementationLibrary? = null): ElideProtocol {
    if (!initialized.get()) {
      // gather installed protocol implementations (one is required at invocation time)
      allProtocols.addAll(ServiceLoader.load(ElideProtocol::class.java)
        .stream()
        .collect(Collectors.toList()))

      // throw if not available
      if (allProtocols.isEmpty()) error("No installed protocol implementations found.")

      // default is the first installed
      val selected = allProtocols.first()
      activeDefaultProtocol = selected.get()
      activeImplType = selected.type()
      initialized.compareAndSet(false, true)
    }
    if (impl == null)
      return activeDefaultProtocol
    return implMap.getOrPut(impl) {
      allProtocols.firstOrNull { it.get().engine() == impl }
        ?.get()
        ?: error("No installed protocol implementation found for $impl.")
    }
  }

  override fun get(): ElideProtocol = acquire(null)

  override fun type(): Class<out ElideProtocol> = activeImplType
}
