package elide.runtime.intrinsics.js

import java.util.concurrent.Phaser
import org.graalvm.polyglot.Context as VMContext

/**
 * # Server Agent
 *
 * Represents an abstract intrinsic which can be used to implement a server agent. This is a special type of intrinsic
 * which is recognized by the Elide CLI/runtime as a server; behavior is triggered which prevents an immediate exit
 * after executing user code.
 */
public interface ServerAgent {
  /**
   * ## Server Initialization
   *
   * Initialize this intrinsic by passing a handle that can later be used to acquire a polyglot context. This is an
   * internal method and should only be used by the JVM entry point.
   *
   * @param contextHandle Handle to the active polyglot context.
   * @param phaserHandle Handle to the phaser used to synchronize the server.
   */
  public fun initialize(contextHandle: VMContext, phaserHandle: Phaser)
}
