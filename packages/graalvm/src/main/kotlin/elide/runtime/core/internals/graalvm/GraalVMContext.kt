/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.core.internals.graalvm

import com.oracle.truffle.js.lang.JavaScriptLanguage
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.util.concurrent.ConcurrentHashMap
import elide.runtime.core.*
import elide.runtime.core.PolyglotContext.EvaluationOptions
import elide.runtime.gvm.internals.js.ELIDE_JS_LANGUAGE_ID
import elide.runtime.gvm.internals.js.ELIDE_TS_LANGUAGE_ID

/**
 * An implementation of the [PolyglotContext] interface wrapping a GraalVM context.
 */
@DelicateElideApi public class GraalVMContext(
  public val context: Context
) : PolyglotContext {
  /** Thread-safe mutable map holding this context's elements. */
  private val elements: MutableMap<PolyglotContextElement<*>, Any?> = ConcurrentHashMap()

  private fun resolveCustomEvaluator(source: Source, options: EvaluationOptions): GuestLanguageEvaluator? {
    return get(GuestLanguageEvaluator.contextElementFor(source.language))?.takeIf { it.accepts(source) }
  }

  private fun resolveCustomParser(source: Source): GuestLanguageParser? {
    return get(GuestLanguageParser.contextElementFor(source.language))?.takeIf { it.accepts(source) }
  }

  override fun bindings(language: GuestLanguage?): PolyglotValue {
    return language?.let {
      // special case: if bindings are requested for typescript or elide JS, we return the bindings for pure JS.
      when (it.languageId) {
        ELIDE_JS_LANGUAGE_ID, ELIDE_TS_LANGUAGE_ID -> context.getBindings(JavaScriptLanguage.ID)
        else -> context.getBindings(it.languageId)
      }
    } ?: context.polyglotBindings
  }

  override fun parse(source: Source): PolyglotValue {
    // prefer using a registered parser for this language, default to using the context
    return resolveCustomParser(source)?.parse(source, this) ?: context.parse(source)
  }

  override fun evaluate(source: Source, options: EvaluationOptions): PolyglotValue {
    // prefer using a registered evaluator for this language, default to using the context
    return resolveCustomEvaluator(source, options)?.evaluate(source, this) ?: context.eval(source)
  }

  override fun enter() {
    context.enter()
  }

  override fun leave() {
    context.leave()
  }

  override operator fun <T> get(element: PolyglotContextElement<T>): T? {
    @Suppress("unchecked_cast") return elements[element] as? T
  }

  override operator fun <T> set(element: PolyglotContextElement<T>, value: T): Boolean {
    return elements.put(element, value) != null
  }
}
