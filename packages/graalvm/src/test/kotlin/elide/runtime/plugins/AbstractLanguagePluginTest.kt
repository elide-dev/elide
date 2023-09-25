package elide.runtime.plugins

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.HostPlatform
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest.EmbeddedResource

@OptIn(DelicateElideApi::class)
internal class AbstractLanguagePluginTest {
  @Test fun testResolveLanguageResources() {
    /** Stubbed plugin allowing access to the resolved language resources. */
    val plugin = object : AbstractLanguagePlugin<Unit, Unit>() {
      override val key: Key<Unit> = Key("TestLanguagePlugin")
      override val languageId: String = "testLang"
      override fun install(scope: InstallationScope, configuration: Unit.() -> Unit) = Unit

      fun resolveResources() = resolveEmbeddedManifest(HostPlatform.resolve())
    }

    val resources = plugin.resolveResources()
    assertContentEquals(
      expected = listOf(
        EmbeddedResource(
          path = "/META-INF/elide/v4/embedded/runtime/testLang/sample-vfs.tar.gz",
          platform = null,
        )
      ),
      actual = resources.bundles,
      message = "should read embedded bundle paths"
    )

    assertContentEquals(
      expected = listOf(
        EmbeddedResource(
          path = "/META-INF/elide/v4/embedded/runtime/testLang/setup.txt",
          platform = null,
        )
      ),
      actual = resources.scripts,
      message = "should read embedded setup script paths"
    )
  }
}
