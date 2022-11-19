package elide.tools.kotlin.plugin

import com.google.auto.service.AutoService
import elide.tools.kotlin.plugin.redakt.PLUGIN_ID as redaktPluginId
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

/**
 * # Plugin: Redakt
 *
 */
@AutoService(CommandLineProcessor::class)
internal class RedaktPlugin : AbstractKotlinPlugin() {
  internal companion object {
    private const val OPTION_STRING = "string"
    private const val OPTION_FILE = "file"

    internal val ARG_STRING = CompilerConfigurationKey<String>(OPTION_STRING)
    internal val ARG_FILE = CompilerConfigurationKey<String>(OPTION_FILE)
  }

  /** @inheritDoc */
  override val pluginId: String get() =  redaktPluginId

  /** @inheritDoc */
  override val pluginOptions: Collection<CliOption> = listOf(
    CliOption(
      optionName = OPTION_STRING,
      valueDescription = "string",
      description = "sample string argument",
      required = false,
    ),
    CliOption(
      optionName = OPTION_FILE,
      valueDescription = "file",
      description = "sample file argument",
      required = false,
    ),
  )

  /** @inheritDoc */
  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration
  ) {
    return when (option.optionName) {
      OPTION_STRING -> configuration.put(ARG_STRING, value)
      OPTION_FILE -> configuration.put(ARG_FILE, value)
      else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
    }
  }
}
