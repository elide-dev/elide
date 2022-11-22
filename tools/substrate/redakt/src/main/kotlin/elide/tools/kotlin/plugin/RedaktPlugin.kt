package elide.tools.kotlin.plugin

import com.google.auto.service.AutoService
import elide.tools.kotlin.plugin.redakt.KEY_ANNOTATION
import elide.tools.kotlin.plugin.redakt.KEY_ENABLED
import elide.tools.kotlin.plugin.redakt.KEY_MASK
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import elide.tools.kotlin.plugin.redakt.PLUGIN_ID as redaktPluginId
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * # Plugin: Redakt
 *
 * Defines the main implementation entrypoint for the Redakt plugin, heavily inspired by Zac Sweers' plugin by a similar
 * name ("Redacted"). See [here](https://github.com/ZacSweers/redacted-compiler-plugin) for Zac's original.
 *
 * The plugin enables processing of the [elide.annotations.data.Sensitive] annotation. Data classes and fields marked
 * with this annotation are redacted from the `toString` representation.
 */
@AutoService(CommandLineProcessor::class)
internal class RedaktPlugin : CommandLineProcessor {
  /** @inheritDoc */
  override val pluginId: String get() = redaktPluginId

  /** @inheritDoc */
  override val pluginOptions: Collection<AbstractCliOption> get() = listOf(
    CliOption(
      optionName = "enabled",
      valueDescription = "<true|false>",
      description = "Enable the Redakt plugin",
      required = false,
      allowMultipleOccurrences = false
    ),
    CliOption(
      optionName = "mask",
      valueDescription = "<mask>",
      description = "The replacement value for masked data",
      required = false,
      allowMultipleOccurrences = false
    ),
    CliOption(
      optionName = "annotation",
      valueDescription = "<annotation>",
      description = "The annotation to use for masking",
      required = false,
      allowMultipleOccurrences = false
    )
  )

  override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
    when (option.optionName) {
      "enabled" -> configuration.put(KEY_ENABLED, value.toBooleanStrictOrNull() ?: true)
      "mask" -> configuration.put(KEY_MASK, value)
      "annotation" -> configuration.put(KEY_ANNOTATION, value)
    }
  }
}
