package elide.tools.kotlin.plugin.redakt

/** Constants used by the Redakt plugin. */
public object RedaktConstants {
  /** Default mask character to use. */
  public const val defaultMaskString: String = "●"

  /** Default annotation to mark sensitive data. */
  public const val defaultSensitiveAnnotation: String = "elide.annotations.data.Sensitive"
}
