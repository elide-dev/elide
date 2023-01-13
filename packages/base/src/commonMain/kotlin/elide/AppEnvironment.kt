package elide

/**
 * Enumerates application environments which can be connected to configuration, secrets, and other behavior.
 *
 * - [SANDBOX]: The application is executing in a development, test, or experimental environment.
 * - [LIVE]: The application is executing in a production environment.
 */
public enum class AppEnvironment {
  /** The application is executing in a development, test, or experimental environment. */
  SANDBOX,

  /** The application is executing in a production environment. */
  LIVE,
}
