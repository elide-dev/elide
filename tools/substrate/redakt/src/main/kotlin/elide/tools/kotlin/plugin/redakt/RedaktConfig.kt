package elide.tools.kotlin.plugin.redakt

import org.jetbrains.kotlin.config.CompilerConfigurationKey

/** Configuration key indicating whether the Redakt plugin is enabled. */
public val KEY_ENABLED: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey<Boolean>(
  "enabled"
)

/** Configuration key specifying the replacement mask string for the Redakt plugin. */
public val KEY_MASK: CompilerConfigurationKey<String> = CompilerConfigurationKey<String>(
  "mask"
)

/** Configuration key specifying the annotation to scan for to trigger the Redakt plugin. */
public val KEY_ANNOTATION: CompilerConfigurationKey<String> = CompilerConfigurationKey<String>(
  "annotation"
)
