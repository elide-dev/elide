package elide.runtime.gvm

import org.graalvm.polyglot.Engine
import java.util.stream.Stream
import elide.runtime.gvm.internals.VMProperty

/**
 * TBD.
 */
public interface ContextFactory<Context, Builder> {
  /**
   * TBD.
   */
  public fun configureVM(props: Stream<VMProperty>)

  /**
   * TBD.
   */
  public fun installContextFactory(factory: (Engine) -> Builder)

  /**
   * TBD.
   */
  public fun installContextSpawn(factory: (Builder) -> Context)

  /**
   * TBD.
   */
  public fun activate(start: Boolean = false)

  /**
   * TBD.
   */
  public fun <R> acquire(builder: ((Builder) -> Unit)? = null, operation: Context.() -> R): R

  /**
   * TBD.
   */
  public operator fun <R> invoke(operation: Context.() -> R): R = acquire(null, operation)
}
