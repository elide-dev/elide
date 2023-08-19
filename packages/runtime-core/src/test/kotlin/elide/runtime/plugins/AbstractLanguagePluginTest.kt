package elide.runtime.plugins

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key

@OptIn(DelicateElideApi::class)
internal class AbstractLanguagePluginTest {
  @Test fun testResolveLanguageResources() {
    /** Stubbed plugin allowing access to the resolved language resources. */
    val plugin = object : AbstractLanguagePlugin<Unit, Unit>() {
      override val key: Key<Unit> = Key("TestLanguagePlugin")
      override val languageId: String = "testLang"
      override fun install(scope: InstallationScope, configuration: Unit.() -> Unit) = Unit

      fun resolveResources() = resolveLanguageResources()
    }

    val resources = plugin.resolveResources()
    assertContentEquals(
      expected = listOf("/META-INF/elide/v4/embedded/runtime/testLang/sample-vfs.tar.gz"),
      actual = resources.bundles,
      message = "should read embedded bundle paths"
    )

    assertContentEquals(
      expected = listOf("/META-INF/elide/v4/embedded/runtime/testLang/setup.txt"),
      actual = resources.setupScripts,
      message = "should read embedded setup script paths"
    )
  }
}
