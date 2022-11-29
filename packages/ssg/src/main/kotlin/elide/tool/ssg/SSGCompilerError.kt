package elide.tool.ssg

/**
 * Error case base class.
 *
 * @param message Message for this error. Required.
 * @param cause Cause of this error. Optional.
 * @param exitCode Exit code to use, as applicable.
 */
public sealed class SSGCompilerError(
  message: String,
  cause: Throwable? = null,
  public val exitCode: Int = -1,
) : Throwable(message, cause) {
  /** Generic error case. */
  public class Generic(cause: Throwable? = null): SSGCompilerError(
    "An unknown error occurred.",
    cause,
  )

  /** Invalid argument error. */
  public class InvalidArgument(message: String, cause: Throwable? = null): SSGCompilerError(
    message,
    cause,
    -2,
  )

  /** I/O error. */
  public class IOError(message: String, cause: Throwable? = null): SSGCompilerError(
    message,
    cause,
    -3,
  )

  /** Output error. */
  public class OutputError(message: String, cause: Throwable? = null): SSGCompilerError(
    message,
    cause,
    -4,
  )
}

