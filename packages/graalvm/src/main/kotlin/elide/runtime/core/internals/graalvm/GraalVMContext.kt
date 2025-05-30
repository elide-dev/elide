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

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.util.concurrent.ConcurrentHashMap
import elide.runtime.core.*
import elide.runtime.core.PolyglotContext.EvaluationOptions

/**
 * An implementation of the [PolyglotContext] interface wrapping a GraalVM context.
 */
@DelicateElideApi public class GraalVMContext(
  public val context: Context
) : PolyglotContext {
  /** Thread-safe mutable map holding this context's elements. */
  private val elements: MutableMap<PolyglotContextElement<*>, Any?> = ConcurrentHashMap()

  @Suppress("UNUSED_PARAMETER")
  private fun resolveCustomEvaluator(source: Source, options: EvaluationOptions): GuestLanguageEvaluator? {
    return get(GuestLanguageEvaluator.contextElementFor(source.language))?.takeIf { it.accepts(source) }
  }

  private fun resolveCustomParser(source: Source): GuestLanguageParser? {
    return get(GuestLanguageParser.contextElementFor(source.language))?.takeIf { it.accepts(source) }
  }

  override fun bindings(language: GuestLanguage?): PolyglotValue {
    try {
      context.enter()
      return language?.let { context.getBindings(it.languageId) } ?: context.polyglotBindings
    } finally {
      context.leave()
    }
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

  override fun unwrap(): Context {
    return context
  }

  override operator fun <T> get(element: PolyglotContextElement<T>): T? {
    @Suppress("unchecked_cast") return elements[element] as? T
  }

  override operator fun <T> set(element: PolyglotContextElement<T>, value: T): Boolean {
    return elements.put(element, value) != null
  }
}
