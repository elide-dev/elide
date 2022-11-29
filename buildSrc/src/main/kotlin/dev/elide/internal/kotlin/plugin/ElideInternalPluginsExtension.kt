package dev.elide.internal.kotlin.plugin

import elide.tools.kotlin.plugin.redakt.RedaktConstants
import org.gradle.api.Project
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/** Configuration for Elide plugins within the scope of the Elide library build. */
open class ElideInternalPluginsExtension @Inject constructor (project: Project) {
  /** Options for the Redakt plugin. */
  public val redakt: PluginConfig.RedaktExtension = project.objects.newInstance(
    PluginConfig.RedaktExtension::class.java
  )

  /** Base configuration extension for plugins. */
  sealed class PluginConfig {
    /** Whether the plugin is enabled. */
    public val enabled: AtomicBoolean = AtomicBoolean(true)

    /** Whether to output extra logging from this plugin. */
    public val verbose: AtomicBoolean = AtomicBoolean(false)

    /** Configuration extension for Redakt. */
    open class RedaktExtension : PluginConfig() {
      /** Mask string to use for redaction. */
      public val mask: AtomicReference<String> = AtomicReference(RedaktConstants.defaultMaskString)

      /** Annotation to scan for. */
      public val annotation: AtomicReference<String> = AtomicReference(RedaktConstants.defaultSensitiveAnnotation)
    }
  }
}
