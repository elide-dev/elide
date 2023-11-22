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

package elide.runtime.plugins.jvm.interop

import kotlin.properties.ReadOnlyProperty
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.plugins.jvm.Jvm

/**
 * Returns a property delegate that lazily loads a guest class by [name], using [loadGuestClass]. Once resolved, the
 * value will be reused.
 *
 * @see loadGuestClass
 */
@DelicateElideApi public fun PolyglotContext.guestClass(name: String): ReadOnlyProperty<Any, PolyglotValue> {
  var cached: PolyglotValue? = null

  return ReadOnlyProperty { _, _ ->
    // early return if already resolved
    cached?.let { return@ReadOnlyProperty it }

    // resolve the guest class from the bindings and cache the returned value
    loadGuestClass(name).also { cached = it }
  }
}

/**
 * Load a class by [name] from the guest context and return it as a [PolyglotValue].
 *
 * Using the [guestClass] extension is preferred over this function since it will cache the result and allows using
 * it as a property delegate.
 *
 * The class will be resolved from the context bindings as specified by the
 * [GraalVM documentation](https://www.graalvm.org/latest/reference-manual/java-on-truffle/interoperability).
 */
@DelicateElideApi public fun PolyglotContext.loadGuestClass(name: String): PolyglotValue {
  return bindings(Jvm).getMember(name)?.takeUnless { it.isNull } ?: error(
    "Failed to resolve guest class <$name>",
  )
}
