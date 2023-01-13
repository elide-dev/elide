package elide.frontend.ssr

/** Context access utility for SSR-shared state. */
public class SSRContext<State: Any> private constructor (
  private val data: State? = null,
) {
  public companion object {
    /** Key where shared state is placed in the execution input data map. */
    public const val STATE: String = "_state_"

    /** Key where combined state is placed in the execution input data map. */
    public const val CONTEXT: String = "_ctx_"

    /** @return SSR context, decoded from the provided input [ctx]. */
    public fun of(ctx: dynamic = null): SSRContext<Any> {
      return if (ctx != null) {
        SSRContext(ctx)
      } else {
        SSRContext()
      }
    }

    /** @return SSR context, decoded from the provided input [ctx], with the provided [ctx] object. */
    public fun <State : Any> typed(ctx: dynamic = null): SSRContext<State> {
      return if (ctx != null) {
        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        SSRContext(ctx as State)
      } else {
        SSRContext()
      }
    }
  }

  /** Info about the current request. */
  public interface RequestInfo {
    /** Request path. */
    public val path: String
  }

  public interface RequestContext : RequestInfo {
    // Nothing at this time.
  }

  /** Execute the provided [fn] within the context of this decoded SSR context. */
  public fun <R> execute(fn: SSRContext<State>.() -> R): R {
    return fn.invoke(this)
  }

  /** @return State container managed by this context. */
  public val state: State? get() {
    return data
  }

  /** @return Active SSR request context. */
  public val context: dynamic get() {
    return data
  }
}
