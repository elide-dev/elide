package elide.runtime.gvm

/**
 * TBD.
 */
public sealed interface ExecutionInputs {
  /**
   * TBD.
   */
  public fun allInputs(): Array<out Any>

  /**
   * TBD.
   */
  public fun buildArguments(): Array<out Any> = allInputs()

  /** Singleton: Empty execution inputs. */
  public object Empty : ExecutionInputs {
    override fun allInputs(): Array<out Any> = emptyArray()
  }

  /** Factory for instances of [ExecutionInputs]. */
  public companion object {
    /** Singleton representing an empty set of execution inputs. */
    @JvmStatic public val EMPTY: ExecutionInputs = Empty
  }
}
