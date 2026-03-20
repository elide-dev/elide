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
package elide.runtime.http.server.python.asgi

import org.graalvm.polyglot.Source

/**
 * Describes an ASGI entrypoint called for every incoming connection received by an [AsgiServerApplication].
 *
 * For every pooled context, the [source] is evaluated once, and an app binding is resolved from it using the specified
 * [bindingName]. If the [bindingArgs] are specified, the binding is treated as a factory and called once to get the
 * actual ASGI application callable.
 *
 * ASGI applications must be async callables accepting `(scope, receive, send)`.
 */
public data class AsgiEntrypoint(
  val source: Source,
  val bindingName: String,
  val bindingArgs: List<Any>? = null,
) {
  public companion object {
    private val SPEC_REGEX = Regex("(?<symbol>\\w+)(?:\\((?<args>.+)\\))?")

    /**
     * Returns an [AsgiEntrypoint] configured from the given import [spec] in the form `<symbol>(<args>)`. Applications
     * that don't need to specify a factory can use `<symbol>` directly.
     */
    @JvmStatic public fun from(spec: String, source: Source): AsgiEntrypoint {
      val match = SPEC_REGEX.find(spec) ?: error("Invalid ASGI spec: $spec")
      val symbol = match.groups["symbol"]?.value ?: error("ASGI spec must contain a symbol name")

      val args = match.groups["args"]?.value?.split(",")
      return AsgiEntrypoint(source, symbol, args)
    }
  }
}
