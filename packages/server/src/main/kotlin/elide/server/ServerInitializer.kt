package elide.server

/** Affixed to classes which initialize Elide server instances. */
public interface ServerInitializer {
  /**
   * Run initialization logic, as applicable.
   */
  public fun initialize() {}
}
