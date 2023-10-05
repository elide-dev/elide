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

package elide.runtime.core

import org.graalvm.polyglot.Source

/**
 * The Polyglot Context is the core of the Elide runtime: it evaluates guest code in the embedded VM, returning the
 * execution result. Context instances can be [acquired][PolyglotEngine.acquire] from a [PolyglotEngine].
 *
 * Contexts are fully generic, meaning they do not impose constraints on the nature of the guest code (as long as the
 * requested language is supported by the context), nor do they infer any execution details from it. This allows
 * specific use cases such as SSR to be built using the context API without coupling.
 *
 * ### Configuration
 *
 * Contexts can be configured using [plugins][EnginePlugin] that subscribe to the
 * [ContextCreated][EngineLifecycleEvent.ContextCreated] event and update the provided [PolyglotContextBuilder].
 *
 * Plugins are installed into the [engine][PolyglotEngine] when it is initialized, and will receive an event for every
 * context issued by [acquire][PolyglotEngine.acquire].
 *
 * ### Guest code execution
 *
 * To evaluate and execute a unit of guest code, call [evaluate] indicating the target [GuestLanguage] and source, the
 * returned [PolyglotValue] will contain the result of the execution, depending on the context configuration.
 */
@DelicateElideApi public interface PolyglotContext {
  /**
   * Returns the root value that provides intrinsic bindings for the specified [language], or all supported languages
   * if [language] is set to `null`.
   *
   * The [putMember][org.graalvm.polyglot.Value.putMember] can be used to add a new top-level binding, which will be
   * accessible in target languages by name, without any import statements.
   */
  public fun bindings(language: GuestLanguage? = null): PolyglotValue

  /**
   * Parse the given [source] without evaluating it, possibly throwing an exception if a syntax error is found.
   * Sources processed in this way may also be cached and reused by later invocations of [parse] or [evaluate].
   *
   * The returned [PolyglotValue] only supports [execute][org.graalvm.polyglot.Value.execute] without arguments.
   *
   * @param source The guest code to be parsed.
   * @return A [PolyglotValue] representing the parsed source, possibly ready for execution.
   */
  public fun parse(source: Source): PolyglotValue
  
  /**
   * Evaluate the given [source], returning the result of the execution. Depending on the configuration of the context,
   * this method may fail if the selected language is not enabled in the underlying engine.
   *
   * @param source The guest code to be executed.
   * @return The result of evaluating the [source].
   */
  public fun evaluate(source: Source): PolyglotValue

  /**
   * Explicitly enter the context in the current thread. This can reduce the overhead when using [evaluate] repeatedly.
   */
  public fun enter()

  /**
   * Explicitly leave the context in the current thread. Do not call this method if a high number of [evaluate] calls
   * is expected; instead, leave once the last call has been received.
   */
  public fun leave()
}

/**
 * Evaluate a fragment of [source] code in the specified [language], returning the result of the execution. Depending
 * on the configuration of the context, this method may fail if the [language] is not enabled in the underlying engine.
 *
 * @param name Name for this source fragment.
 * @param language The language of the [source] code.
 * @param source The guest code to be executed.
 * @param internal Indicates that the source in question is "internal."
 * @return The result of evaluating the [source].
 */
@DelicateElideApi public fun PolyglotContext.evaluate(
  language: GuestLanguage,
  source: String,
  name: String? = null,
  internal: Boolean = false,
): PolyglotValue {
  return evaluate(
    Source.newBuilder(language.languageId, source, name ?: "(inline)")
      .internal(internal)
      .build()
  )
}
