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
 * @param language The language of the [source] code.
 * @param source The guest code to be executed.
 * @return The result of evaluating the [source].
 */
@DelicateElideApi public fun PolyglotContext.evaluate(language: GuestLanguage, source: String): PolyglotValue {
  return evaluate(Source.create(language.languageId, source))
}
