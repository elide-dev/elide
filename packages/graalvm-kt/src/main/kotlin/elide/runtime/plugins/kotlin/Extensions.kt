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

package elide.runtime.plugins.kotlin

import org.graalvm.polyglot.Source
import java.io.File
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.core.evaluate

/**
 * Execute the given Kotlin [source] code as an interactive snippet, returning the result. This is equivalent to
 * calling [PolyglotContext.evaluate] and selecting [Kotlin] as source language with an interactive source.
 *
 * @param source The Kotlin snippet to be evaluated.
 * @return The result of the invocation.
 */
@DelicateElideApi public fun PolyglotContext.kotlin(source: String, name: String? = null): PolyglotValue {
  return evaluate(
    Source.newBuilder(Kotlin.languageId, source, name ?: "snippet.kts")
      .interactive(true)
      .build(),
  )
}

/**
 * Execute the given [source] code as a Kotlin Script, returning the result. This is equivalent to calling
 * [PolyglotContext.evaluate] and selecting [Kotlin] as source language with a non-interactive source.
 *
 * @param source The source code of the Kotlin Script.
 * @return The result of the invocation.
 */
@DelicateElideApi public fun PolyglotContext.kotlinScript(source: String, name: String? = null): PolyglotValue {
  return evaluate(Source.newBuilder(Kotlin.languageId, source, name ?: "snippet.kts").build())
}

/**
 * Execute the given source [file] as a Kotlin Script, returning the result. This is equivalent to calling
 * [PolyglotContext.evaluate] and selecting [Kotlin] as source language with a non-interactive source.
 *
 * @param file A basic Kotlin Script source file (.kts)
 * @return The result of the invocation.
 */
@DelicateElideApi public fun PolyglotContext.kotlinScript(file: File): PolyglotValue {
  return evaluate(Source.newBuilder(Kotlin.languageId, file).build())
}