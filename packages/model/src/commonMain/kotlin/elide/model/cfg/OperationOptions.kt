package elide.model.cfg

/**
 * Operational options that can be applied to individual calls into the `ModelAdapter` framework. See individual options
 * interfaces for more information.
 */
public interface OperationOptions {
  /** @return Value to apply to the operation timeout. If left unspecified, the global default is used. */
  public fun timeoutValueMilliseconds(): Long? {
    return null
  }

  /** @return Set a precondition for the precise time (in microseconds) that a record was updated. */
  public fun updatedAtMicros(): Long? {
    return null
  }

  /** @return Set a precondition for the precise time (in seconds) that a record was updated. */
  public fun updatedAtSeconds(): Long? {
    return null
  }

  /** @return Number of retries, otherwise the default is used. */
  public fun retries(): Int? {
    return null
  }

  /** @return Whether to run in a transaction. */
  public fun transactional(): Boolean {
    return false
  }
}
