package dev.elide.internal.kotlin.plugin

import dev.elide.internal.ElideInternalPlugin.ElideInternalExtension
import dev.elide.internal.kotlin.plugin.ElideInternalPluginsExtension.PluginConfig.RedaktExtension
import elide.tools.kotlin.plugin.redakt.RedaktConstants.pluginId
import elide.tools.kotlin.plugin.redakt.RedaktConstants.pluginVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import javax.inject.Inject

/** Applies the Redakt plugin to Elide packages. */
internal class InternalRedaktPlugin @Inject constructor (
  project: Project
) : AbstractInternalPluginHost<RedaktExtension>(project, pluginId, pluginVersion) {
  override fun resolve(config: ElideInternalPluginsExtension): RedaktExtension = config.redakt

  override fun configure(extension: ElideInternalExtension, project: Project): List<SubpluginOption> = listOf(
    SubpluginOption(
      key = "enabled",
      value = options.enabled.get().toString(),
    ),
    SubpluginOption(
      key = "mask",
      value = options.mask.get(),
    ),
    SubpluginOption(
      key = "annotation",
      value = options.annotation.get(),
    ),
  )
}
