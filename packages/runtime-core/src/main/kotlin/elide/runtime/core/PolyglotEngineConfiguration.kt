package elide.runtime.core

/**
 * This class acts as the root of the engine configuration DSL, allowing plugins to be
 * [installed][PluginRegistry.install] and exposing general features such as
 * [enabling support for specific languages][enableLanguage].
 *
 * Instances of this class cannot be created manually, instead, they are provided by the [PolyglotEngine] method, which
 * serves as entry point for the DSL.
 */
@DelicateElideApi public abstract class PolyglotEngineConfiguration internal constructor(): PluginRegistry {
  /** Enables support for the specified [language] on all contexts created by the engine. */
  public abstract fun enableLanguage(language: GuestLanguage)
}