package elide.embedded

/**
 * A registry for guest applications.
 */
public interface EmbeddedAppRegistry {
  /**
   * Register a new guest app using the specified [id] and [config], returning an [EmbeddedApp] reference that can be
   * used to track the state of the application.
   *
   * If a guest application with the same [id] already exists, an exception will be thrown.
   */
  public fun register(id: EmbeddedAppId, config: EmbeddedAppConfiguration): EmbeddedApp

  /**
   * Remove a guest app with the specified [id] from the registry and [cancel][EmbeddedApp.cancel] it. The application
   * will be unusable after this operation completes.
   */
  public fun remove(id: EmbeddedAppId): Boolean

  /**
   * Returns a guest [EmbeddedApp] with the specified [id] in this registry, or `null` if no application has been
   * registered with that key.
   */
  public fun resolve(id: EmbeddedAppId): EmbeddedApp?

  /**
   * Close the registry, removing and cancelling every registered app. Attempting to [register] new applications after
   * cancellation will throw an exception.
   */
  public fun cancel(): Boolean
}
