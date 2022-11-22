package dev.elide.buildtools.gradle.plugin.kotlin

import dev.elide.buildtools.gradle.plugin.ElideExtension
import dev.elide.buildtools.gradle.plugin.cfg.ElideKotlinPluginsHandler
import dev.elide.buildtools.gradle.plugin.cfg.ElideKotlinPluginsHandler.PluginHandler.RedaktHandler
import elide.annotations.core.Internal
//import elide.tools.kotlin.plugin.redakt.RedaktConstants.pluginId
import elide.tools.kotlin.plugin.redakt.RedaktConstants.defaultMaskString
import elide.tools.kotlin.plugin.redakt.RedaktConstants.defaultSensitiveAnnotation
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/** Host plugin for the Redakt compiler plugin for Kotlin. */
// @TODO(sgammon): symbolic plugin ID
@Internal internal class RedaktPluginHost : AbstractCompilerPluginHost<RedaktHandler>("redakt") {
    /** @inheritDoc */
    override fun resolve(config: ElideKotlinPluginsHandler): RedaktHandler = config.redaktOptions

    /** @inheritDoc */
    override fun configure(extension: ElideExtension, project: Project): List<SubpluginOption> = listOf(
        SubpluginOption(
            key = "enabled",
            value = options.enabled.get().toString()
        ),
        SubpluginOption(
            key = "mask",
            value = options.mask.get() ?: defaultMaskString,
        ),
        SubpluginOption(
            key = "annotation",
            value = options.annotation.get() ?: defaultSensitiveAnnotation,
        ),
    )
}
