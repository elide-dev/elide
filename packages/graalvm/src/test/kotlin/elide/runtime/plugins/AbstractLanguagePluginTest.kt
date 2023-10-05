/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.plugins

import org.junit.jupiter.api.Test
import kotlin.test.Ignore
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
          path = "/META-INF/elide/embedded/runtime/testLang/sample-vfs.tar.gz",
          platform = null,
        )
      ),
      actual = resources.bundles,
      message = "should read embedded bundle paths"
    )

    assertContentEquals(
      expected = listOf(
        EmbeddedResource(
          path = "/META-INF/elide/embedded/runtime/testLang/setup.txt",
          platform = null,
        )
      ),
      actual = resources.scripts,
      message = "should read embedded setup script paths"
    )
  }
}
