package elide.tool.cli.err

/** Enumerates error cases that may arise during shell execution. */
@Suppress("unused") internal enum class ShellError : ToolErrorCase<ShellError> {
  /** Thrown when a requested language is not supported. */
  LANGUAGE_NOT_SUPPORTED,

  /** Thrown when an error occurs while executing user code. */
  USER_CODE_ERROR,

  /** A file could not be found. */
  FILE_NOT_FOUND,

  /** The target provided is not a file. */
  NOT_A_FILE,

  /** A file is not readable. */
  FILE_NOT_READABLE,

  /** A file type is mismatched with a guest language. */
  FILE_TYPE_MISMATCH,
}
