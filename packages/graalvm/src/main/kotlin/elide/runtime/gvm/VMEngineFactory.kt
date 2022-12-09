package elide.runtime.gvm

/**
 * TBD.
 */
public interface VMEngineFactory<Engine: VMEngine> {
  /**
   *
   */
  public fun acquire(): Engine
}
