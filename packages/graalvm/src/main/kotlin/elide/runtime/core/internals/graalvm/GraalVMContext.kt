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

package elide.runtime.core.internals.graalvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.util.concurrent.ConcurrentHashMap
import elide.runtime.core.*

/**
 * An implementation of the [PolyglotContext] interface wrapping a GraalVM context.
 */
@DelicateElideApi internal class GraalVMContext(val context: Context) : PolyglotContext {
  /** Thread-safe mutable map holding this context's elements. */
  private val elements: MutableMap<PolyglotContextElement<*>, Any?> = ConcurrentHashMap()

  private fun resolveCustomEvaluator(source: Source): GuestLanguageEvaluator? {
    return get(GuestLanguageEvaluator.contextElementFor(source.language))
  }

  private fun resolveCustomParser(source: Source): GuestLanguageParser? {
    return get(GuestLanguageParser.contextElementFor(source.language))
  }

  override fun bindings(language: GuestLanguage?): PolyglotValue {
    return language?.let { context.getBindings(it.languageId) } ?: context.polyglotBindings
  }

  override fun parse(source: Source): PolyglotValue {
    // prefer using a registered parser for this language, default to using the context
    return resolveCustomParser(source)?.parse(source, this) ?: context.parse(source)
  }

  override fun evaluate(source: Source): PolyglotValue {
    // prefer using a registered evaluator for this language, default to using the context
    return resolveCustomEvaluator(source)?.evaluate(source, this) ?: context.eval(source)
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
