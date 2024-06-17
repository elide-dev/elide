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
package elide.runtime.plugins.js

import org.graalvm.polyglot.Source
import java.net.URI
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.core.evaluate

/**
 * Execute the given JavaScript [source] code, returning the result. This is equivalent to calling
 * [PolyglotContext.evaluate] and selecting [JavaScript] as source language.
 *
 * @param source The JavaScript source code to be executed.
 * @param esm Whether to treat the [source] as an ESM module. If false, the code is evaluated as CommonJS source.
 * @param name Name to provide for this script.
 * @param internals Whether to treat this code as "internal" to the runtime; this also exposes `primordials`.
 * @param interactive Whether this script should run interactively; defaults to `false`.
 * @param cached Whether to allow source-base caching; defaults to `true`.
 * @param uri Addressable URI to this source code; defaults to `null`. Generated on-the-fly if not provided.
 * @param unlockInternals Unlocks internal APIs even if a source script is not part of the runtime [internals]; this is
 *    mostly useful for testing and benchmarking/debugging.
 * @return The result of the invocation. If [esm] is `true`, an object is returned, with exported values as members.
 */
@DelicateElideApi public fun PolyglotContext.javascript(
  source: String,
  esm: Boolean = false,
  name: String? = null,
  internals: Boolean = false,
  interactive: Boolean = false,
  cached: Boolean = true,
  uri: URI? = null,
  unlockInternals: Boolean = false,
): PolyglotValue {
  return evaluate(Source.newBuilder(
    /* language = */ JavaScript.languageId,
    /* characters = */ source,
    /* name = */ name ?: (if (esm) "source.mjs" else "source.js"),
  ).apply {
    internal(internals)
    interactive(interactive)
    cached(cached)
    uri?.let { uri(it) }
  }.build(), internals || unlockInternals)
}

/**
 * Execute the given JavaScript [Source], returning the result. This is equivalent to calling
 * [PolyglotContext.evaluate] and selecting [JavaScript] as source language.
 *
 * @param source The interpreted JavaScript source code to be executed.
 * @return The result of the invocation. If [esm] is `true`, an object is returned, with exported values as members.
 */
@DelicateElideApi public fun PolyglotContext.javascript(source: Source): PolyglotValue =
  evaluate(source)
