package elide.runtime

/** Describes different sources for an environment variable. */
public enum class EnvVariableSource {
  /** The value is provided explicitly, inline. */
  INLINE,

  /** The value originates from a `.env` file. */
  DOTENV,

  /** The value is resolved from the host environment. */
  HOST
}
