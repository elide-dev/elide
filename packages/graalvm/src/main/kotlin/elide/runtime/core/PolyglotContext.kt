package elide.runtime.core

/**
 * The Polyglot Context is the core of the Elide runtime: it evaluates guest code in the embedded VM, returning the
 * execution result. Context instances can be [acquired][PolyglotEngine.acquire] from a [PolyglotEngine].
 *
 * Contexts are fully generic, meaning they do not impose constraints on the nature of the guest code (as long as the
 * requested language is supported by the context), nor do they infer any execution details from the it. This allows
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
 * To evaluate and execute a unit of guest code, call [execute] indicating the target [GuestLanguage] and source, the
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
   * Evaluate a unit of guest code in the given [language], returning the result of the execution. Depending on the
   * configuration of the context, this method may fail if the [language] is not enabled in the underlying engine.
   *
   * @param language The language of the [source] code to be evaluated.
   * @param source The guest code to be executed.
   * @return The result of evaluating [source].
   */
  public fun execute(language: GuestLanguage, source: String): PolyglotValue
}
