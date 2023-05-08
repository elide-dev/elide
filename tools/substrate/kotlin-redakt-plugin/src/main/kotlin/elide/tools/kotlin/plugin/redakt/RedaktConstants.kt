package elide.tools.kotlin.plugin.redakt

/** Constants used by the Redakt plugin. */
public object RedaktConstants {
  /** Plugin ID. */
  public const val pluginId: String = PLUGIN_ID

  /** Active plugin version. */
  public const val pluginVersion: String = PLUGIN_VERSION

  /** Default mask character to use. */
  public const val defaultMaskString: String = "●●●●"

  /** Default annotation to mark sensitive data. */
  public const val defaultSensitiveAnnotation: String = "elide.annotations.data.Sensitive"
}
