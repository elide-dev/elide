package elide.embedded

/**
 * Describes the language used by the guest application, which must be supported by the runtime at the time of
 * registration. Source language affects the bindings available at run-time as well as other dispatch features.
 */
public enum class EmbeddedGuestLanguage {
  /** Use JavaScript as guest language. */
  JAVA_SCRIPT,

  /** Use Python as guest language. */
  PYTHON
}
