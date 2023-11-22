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

package elide.runtime.plugins.kotlin.shell

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.plugins.jvm.interop.loadGuestClass

/**
 * A wrapper around a [PolyglotValue], used to interface with a dynamic class loader instance in a guest context. The
 * guest loader allows defining a new class by providing the bytecode.
 *
 * This wrapper is used by the [GuestKotlinScriptEvaluator] to define the compiled script classes in the guest context
 * before evaluating them.
 *
 * @see GuestKotlinScriptEvaluator
 */
@DelicateElideApi @JvmInline internal value class GuestClassLoader(private val delegate: PolyglotValue) {
  /** Create a new instance by resolving the guest class from a [context]. */
  constructor(context: PolyglotContext) : this(context.loadGuestClass(DYNAMIC_LOADER_CLASS).newInstance())

  /** Define a class in the guest context using its [name] and [bytecode]. */
  fun defineClass(name: String, bytecode: PolyglotValue) {
    delegate.invokeMember("define", name, bytecode)
  }

  /** Load a class from the guest context by [Z]. */
  fun loadClass(name: String): PolyglotValue {
    return delegate.invokeMember("loadClass", name)
  }

  private companion object {
    /** Fully qualified class name used to resolve the dynamic loader class in a guest context. */
    private const val DYNAMIC_LOADER_CLASS = "elide.runtime.plugins.kotlin.shell.DynamicClassLoader"
  }
}
