package elide.embedded

/** A specialized DI container used by the embedded runtime to resolve core services. */
public interface EmbeddedRuntimeContext {
  /** Runtime configuration used by this context. */
  public val configuration: EmbeddedConfiguration

  /** The [EmbeddedAppRegistry] used by the runtime. */
  public val appRegistry: EmbeddedAppRegistry

  /** The call dispatcher used by the runtime. */
  public val dispatcher: EmbeddedCallDispatcher
}