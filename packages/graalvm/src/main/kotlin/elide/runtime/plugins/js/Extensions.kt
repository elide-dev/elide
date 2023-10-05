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

package elide.runtime.plugins.js

import org.graalvm.polyglot.Source
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.core.evaluate

/**
 * Execute the given JavaScript [source] code, returning the result. This is equivalent to calling
 * [PolyglotContext.evaluate] and selecting [JavaScript] as source language.
 *
 * @param source The JavaScript source code to be executed.
 * @param esm Whether to treat the [source] as an ESM module. If false, the code is evaluted as CommonJS source.
 * @param name Name to provide for this script.
 * @return The result of the invocation. If [esm] is `true`, an object is returned, with exported values as members.
 */
@DelicateElideApi public fun PolyglotContext.javascript(
  source: String,
  esm: Boolean = false,
  name: String? = null,
): PolyglotValue {
  val src = Source.newBuilder(
    /* language = */ JavaScript.languageId,
    /* characters = */ source,
    /* name = */ name ?: (if (esm) "source.mjs" else "source.js"),
   ).build()

  return evaluate(src)
}
