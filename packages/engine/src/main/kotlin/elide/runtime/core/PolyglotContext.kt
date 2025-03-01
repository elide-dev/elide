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

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.net.URI

// Static defaults for polyglot methods.
private object PolyglotDefaults {
  // Whether to consider sources as "interactive" by default.
  const val DEFAULT_INTERACTIVE: Boolean = false

  // Whether to enable parser and interpreter caching by default.
  const val DEFAULT_CACHED: Boolean = true
}

/**
 * Evaluate a fragment of [source] code in the specified [language], returning the result of the execution. Depending
 * on the configuration of the context, this method may fail if the [language] is not enabled in the underlying engine.
 *
 * @param name Name for this source fragment.
 * @param language The language of the [source] code.
 * @param source The guest code to be executed.
 * @param internals Indicates that the source in question is "internal."
 * @param interactive Whether this script should run interactively; defaults to `false`.
 * @param cached Whether to allow source-base caching; defaults to `true`.
 * @param uri Addressable URI to this source code; defaults to `null`. Generated on-the-fly if not provided.
 * @return The result of evaluating the [source].
 */
@DelicateElideApi public fun PolyglotContext.evaluate(
  language: GuestLanguage,
  source: String,
  name: String? = null,
  internals: Boolean = false,
  interactive: Boolean = PolyglotDefaults.DEFAULT_INTERACTIVE,
  cached: Boolean = PolyglotDefaults.DEFAULT_CACHED,
  uri: URI? = null,
): PolyglotValue = evaluate(
  Source.newBuilder(language.languageId, source, name ?: "(inline)").apply {
    internal(internals)
    interactive(interactive)
    cached(cached)
    uri?.let { uri(it) }
  }.build(),
)

/**
 * Parse a fragment of [source] code in the specified [language], returning the parsed value, which is typically
 * executable. Depending on the configuration of the context, this method may fail if the [language] is not enabled in
 * the underlying engine.
 *
 * @param name Name for this source fragment.
 * @param language The language of the [source] code.
 * @param source The guest code to be executed.
 * @param internals Indicates that the source in question is "internal."
 * @param interactive Whether this script should run interactively; defaults to `false`.
 * @param cached Whether to allow source-base caching; defaults to `true`.
 * @param uri Addressable URI to this source code; defaults to `null`. Generated on-the-fly if not provided.
 * @return The result of evaluating the [source].
 */
@DelicateElideApi public fun PolyglotContext.parse(
  language: GuestLanguage,
  source: String,
  name: String? = null,
  internals: Boolean = false,
  interactive: Boolean = PolyglotDefaults.DEFAULT_INTERACTIVE,
  cached: Boolean = PolyglotDefaults.DEFAULT_CACHED,
  uri: URI? = null,
): PolyglotValue = parse(
  Source.newBuilder(language.languageId, source, name ?: "(inline)").apply {
    internal(internals)
    interactive(interactive)
    cached(cached)
    uri?.let { uri(it) }
  }.build(),
)

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
@DelicateElideApi
public interface PolyglotContext {
  /**
   * ## Evaluation Options
   *
   * Specifies options related to the [evaluate] step for a given chunk of guest code; these options can be provided
   * optionally at the call-site to control the behavior of the evaluation.
   */
  public interface EvaluationOptions {
    /**
     * Whether this source is "internal" to the runtime; such sources are hidden from tools like debuggers by default,
     * and granted access to internal runtime features.
     */
    public val internals: Boolean

    /** Factories for creating [EvaluationOptions]. */
    public companion object {
      /** @return [EvaluationOptions] for the provided parameters. */
      @JvmStatic public fun of(internals: Boolean): EvaluationOptions = EvaluationOptionsData(internals)
    }
  }

  /** Default implementation of [EvaluationOptions]. */
  @ConsistentCopyVisibility
  @JvmRecord public data class EvaluationOptionsData internal constructor (
    override val internals: Boolean,
  ) : EvaluationOptions

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
   * This is the main variant that implementors are expected to provide; the enclosed [options] should carry all
   * defaults as well as user overrides.
   *
   * @param source The guest code to be executed.
   * @param options Full suite of options applying to this evaluation, with defaults filled in, as applicable.
   * @return The result of evaluating the [source].
   */
  public fun evaluate(source: Source, options: EvaluationOptions): PolyglotValue

  /**
   * Evaluate the given [source], returning the result of the execution. Depending on the configuration of the context,
   * this method may fail if the selected language is not enabled in the underlying engine.
   *
   * NOTE: This method merely prepares [EvaluationOptions] accordingly, and then dispatches to the main variant of
   * [evaluate].
   *
   * @param source The guest code to be executed.
   * @param internals Whether to allow access to internal runtime features; the provided [source] must be marked as
   *   internal to enable this acesss.
   * @return The result of evaluating the [source].
   */
  public fun evaluate(source: Source, internals: Boolean): PolyglotValue =
    evaluate(source, EvaluationOptions.of(internals))

  /**
   * Evaluate the given [source], returning the result of the execution. Depending on the configuration of the context,
   * this method may fail if the selected language is not enabled in the underlying engine.
   *
   * NOTE: This method does not specify any additional access grants or other options for the evaluation cycle; see
   * [EvaluationOptions] and other variants of [evaluate] for more information.
   *
   * @param source The guest code to be executed.
   * @return The result of evaluating the [source].
   */
  public fun evaluate(source: Source): PolyglotValue = evaluate(source, internals = true)

  /**
   * Explicitly enter the context in the current thread. This can reduce the overhead when using [evaluate] repeatedly.
   */
  public fun enter()

  /**
   * Explicitly leave the context in the current thread. Do not call this method if a high number of [evaluate] calls
   * is expected; instead, leave once the last call has been received.
   */
  public fun leave()

  /** @return The underlying GraalVM context. */
  public fun unwrap(): Context

  /** Returns the value associated with a given context [element], or `null` if the element is not present. */
  public operator fun <T> get(element: PolyglotContextElement<T>): T?

  /** Sets the value associated with a given context [element], returning `true` if a previous value was replaced. */
  public operator fun <T> set(element: PolyglotContextElement<T>, value: T): Boolean
}
