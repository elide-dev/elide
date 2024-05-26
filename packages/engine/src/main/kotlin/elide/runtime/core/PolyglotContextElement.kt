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
package elide.runtime.core

/**
 * A type-safe key used to store and retrieve values in a [PolyglotContext].
 *
 * Engine plugins can manage elements with [PolyglotContext.set] and [PolyglotContext.get] in order to share and reuse
 * values created during context initialization. For example, a language plugin providing REPL support might create
 * the shell evaluator and store it using an element key, which would allow an extension to access a context-bound
 * instance when necessary.
 *
 * Note that the Elements API is meant to be used only by host code, and is not available inside the guest context. If
 * you need to share values with guest code, use [bindings][PolyglotContext.bindings] instead.
 *
 * @see PolyglotContext
 */
@DelicateElideApi
@JvmInline public value class PolyglotContextElement<T>(public val key: String)
