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

package elide.runtime.plugins

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.GuestLanguage
import elide.runtime.core.PolyglotContext

/**
 * Base class for configuration DSL elements used by language plugins. This class adds support for general features
 * such as intrinsic bindings.
 *
 * @see AbstractLanguagePlugin
 */
@DelicateElideApi public abstract class AbstractLanguageConfig {
  /** Mutable counterpart to [intrinsicBindings]. */
  private val mutableBindings: MutableMap<String, Any> = mutableMapOf()

  /** An immutable map of the intrinsics defined using the [bindings] function. */
  protected val intrinsicBindings: Map<String, Any> get() = mutableBindings

  /**
   * Configure intrinsic bindings for this language. These bindings will be available as top-level symbols in every
   * context, with the names provided by the specified keys.
   */
  public fun bindings(block: MutableMap<String, Any>.() -> Unit) {
    mutableBindings.apply(block)
  }

  /**
   * Apply the language [bindings][AbstractLanguageConfig.bindings] defined in this configuration to the target,
   * [context], optionally scoping them to a specified [language].
   *
   * @param context The context to apply the bindings to.
   * @param language The language scope for the bindings. If `null`, bindings are applied to all languages.
   */
  @DelicateElideApi protected fun applyBindings(context: PolyglotContext, language: GuestLanguage? = null) {
    with(context.bindings(language)) {
      intrinsicBindings.forEach { entry -> putMember(entry.key, entry.value) }
    }
  }
}
