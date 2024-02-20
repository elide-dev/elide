package elide.embedded

/** A specialized DI container used by the embedded runtime to resolve core services. */
public interface EmbeddedRuntimeContext {
  /** The [EmbeddedAppRegistry] used by the runtime. */
  public val appRegistry: EmbeddedAppRegistry
}