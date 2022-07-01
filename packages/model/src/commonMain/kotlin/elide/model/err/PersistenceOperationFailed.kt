package elide.model.err

/** Describes a generic operational failure that occurred within the persistence engine.  */
public class PersistenceOperationFailed private constructor(
  /** Enumerated failure case.  */
  public val failure: PersistenceFailure,
  message: String,
  cause: Throwable?
): PersistenceException(message, cause) {
  public companion object {
    /**
     * Generate a persistence failure exception for a generic (known) failure case.
     *
     * @param failure Known failure case to spawn an exception for.
     * @return Exception object.
     */
    public fun forErr(failure: PersistenceFailure): PersistenceOperationFailed {
      return PersistenceOperationFailed(failure, failure.message, null)
    }

    /**
     * Generate a persistence failure exception for a generic (known) failure case, optionally applying an inner cause
     * exception to the built object.
     *
     * @param failure Known failure case to spawn an exception for.
     * @param cause Exception object to use as the inner cause.
     * @return Spawned persistence exception object.
     */
    public fun forErr(failure: PersistenceFailure, cause: Throwable?): PersistenceOperationFailed {
      return PersistenceOperationFailed(failure, failure.message, cause)
    }
  }
}
