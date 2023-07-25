package elide.runtime.intrinsics.js.express

import elide.annotations.core.Polyglot

/**
 * Interface used as the root `express` module, which exports a function as default value. Invoking the function
 * returns a new [ExpressApp].
 */
public interface Express {
  /**
   * Initialize this intrinsic by passing a handle that can later be used to acquire a polyglot context. This is an
   * internal method and should only be used by the JVM entry point.
   */
  public fun initialize(contextHandle: Any, phaserHandle: Any)
  
  /** Returns a new [ExpressApp] instance. This is the default export for the express module. */
  @Polyglot public operator fun invoke(): ExpressApp
}