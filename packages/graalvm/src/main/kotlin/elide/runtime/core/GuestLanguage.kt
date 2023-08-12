package elide.runtime.core

/**
 * Represents a language supported by a [PolyglotContext], providing a key for identification of guest bindings and
 * other contextual elements.
 */
@DelicateElideApi public interface GuestLanguage {
  /** A unique string representing this language. */
  public val languageId: String
}