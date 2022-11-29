import Elide
import dev.elide.internal.ElideInternalPlugin
import dev.elide.internal.kotlin.plugin.InternalRedaktPlugin

apply<ElideInternalPlugin>()

the<ElideInternalPlugin.ElideInternalExtension>().apply {
  // Library version.
  version.set(Elide.version)

  kotlinPlugins.apply {
    // Plugin: Redakt
    redakt.enabled.set(true)
    redakt.verbose.set(true)
    redakt.mask.set("●●●●")
    redakt.annotation.set("elide.annotations.data.Sensitive")
  }
}

apply<InternalRedaktPlugin>()
