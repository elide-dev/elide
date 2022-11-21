package elide.tools.kotlin.plugin

import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

/**
 *
 */
public abstract class AbstractKotlinPlugin (
  protected val name: String,
) : CommandLineProcessor {
  /** @inheritDoc */
  override val pluginId: String get() =  name
}
