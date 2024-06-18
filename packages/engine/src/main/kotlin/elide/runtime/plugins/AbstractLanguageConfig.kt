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
package elide.runtime.plugins

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyObject
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
  public companion object {
    private const val EXPERIMENTAL_SECURE_INTERNALS = true
  }

  /** Mutable counterpart to [intrinsicBindings]. */
  private val mutableBindings: MutableMap<String, Any> = mutableMapOf()

  /** An immutable map of the intrinsics defined using the [bindings] function. */
  protected val intrinsicBindings: Map<String, Any> get() = mutableBindings

  /** Executable which should be returned by `sys.executable`. */
  public var executable: String? = null

  /** Full suite of executable args as presented by `sys.argv`. */
  public var executableList: List<String>? = null

  /** Path to native libraries and resources. */
  public var resourcesPath: String? = null

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
      val internals = HashMap<String, Any>()

      intrinsicBindings.forEach { entry ->
        // @TODO: don't unconditionally mount all members
        val isInternal = entry.key.startsWith("__Elide")
        if (!EXPERIMENTAL_SECURE_INTERNALS || !isInternal) {
          putMember(entry.key, entry.value)
        }
        if (isInternal) {
          internals[entry.key.removePrefix("__Elide_").removeSuffix("__")] = entry.value
        }
      }

      // mount internals at `primordials`
      val internalKeys = internals.keys.toTypedArray()
      putMember("primordials", object : ProxyObject, ProxyHashMap {
        override fun getMemberKeys(): Array<String> = internalKeys
        override fun hasMember(key: String?): Boolean = key != null && key in internalKeys
        override fun hasHashEntry(key: Value?): Boolean = key != null && key.asString() in internalKeys
        override fun getHashSize(): Long = internalKeys.size.toLong()

        override fun putMember(key: String?, value: Value?) {
          // no-op
        }

        override fun putHashEntry(key: Value?, value: Value?) {
          // no-op
        }

        override fun removeMember(key: String?): Boolean {
          return false // not supported
        }

        override fun removeHashEntry(key: Value?): Boolean {
          return false // not supported
        }

        override fun getMember(key: String?): Any? = when (key) {
          null -> null
          else -> internals[key]
        }

        override fun getHashValue(key: Value?): Any? = when (key) {
          null -> null
          else -> internals[key.asString()]
        }

        override fun getHashEntriesIterator(): Any = internals.iterator()
      })
    }
  }
}
