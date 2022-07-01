package elide.model.err

/** Enumerates common kinds of persistence failures. Pairs well with [PersistenceOperationFailed]. */
public enum class PersistenceFailure {
  /** The operation timed out.  */
  TIMEOUT,

  /** The operation was cancelled.  */
  CANCELLED,

  /** The operation was interrupted.  */
  INTERRUPTED,

  /** An unknown internal error occurred.  */
  INTERNAL;

  /** @return Error message for the selected case.*/
  public val message: String
    get() = when (this) {
      TIMEOUT -> "The operation timed out."
      CANCELLED -> "The operation was cancelled."
      INTERRUPTED -> "The operation was interrupted."
      else -> "An unknown internal error occurred."
    }
}
