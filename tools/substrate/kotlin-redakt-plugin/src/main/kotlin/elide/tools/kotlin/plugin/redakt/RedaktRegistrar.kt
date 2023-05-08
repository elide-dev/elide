@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
@file:Suppress("DEPRECATION")

package elide.tools.kotlin.plugin.redakt

import com.google.auto.service.AutoService
import elide.tools.kotlin.plugin.RedaktPlugin
import elide.tools.kotlin.plugin.redakt.fir.FirRedaktExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.ClassId

/** Provides a component registry for the Redakt plugin. */
@AutoService(ComponentRegistrar::class)
internal class RedaktRegistrar(
  private val defaultMaskString: String,
  private val defaultAnnotation: String,
) : ComponentRegistrar {
  @Suppress("unused")  // Used by service loader
  constructor() : this(
    defaultMaskString = RedaktConstants.defaultMaskString,
    defaultAnnotation = RedaktConstants.defaultSensitiveAnnotation,
  )

  override val supportsK2: Boolean get() = true

  /** @inheritDoc */
  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration
  ) {
    val enabled = configuration[KEY_ENABLED] ?: true
    if (!enabled) return

    val mask = configuration[KEY_MASK] ?: defaultMaskString
    val annotation = configuration[KEY_ANNOTATION] ?: defaultAnnotation

    val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    val annoClassId = ClassId.fromString(annotation)
    val annoFq = annoClassId.asSingleFqName()

    // mount IR gen extension
    IrGenerationExtension.registerExtension(
      project,
      RedaktIrExtension(
        messageCollector,
        mask,
        annoFq.asString(),
      ),
    )

    // mount FIR extension
    FirExtensionRegistrarAdapter.registerExtension(
      project,
      FirRedaktExtensionRegistrar.forAnnotation(
        annoClassId
      ),
    )
  }
}
