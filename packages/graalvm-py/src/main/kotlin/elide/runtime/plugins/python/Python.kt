/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.runtime.plugins.python

import org.graalvm.polyglot.io.FileSystem
import java.nio.file.Path
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.extensions.disableOptions
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.extensions.setOptions
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest
import elide.runtime.vfs.LanguageVFS.LanguageVFSInfo
import elide.runtime.vfs.registerLanguageVfs

@DelicateElideApi public class Python(
  private val config: PythonConfig,
  private val resources: LanguagePluginManifest? = null,
) {
  private fun initializeContext(context: PolyglotContext) {
    // apply init-time settings
    config.applyTo(context)

    // run embedded initialization code
    if (resources != null) initializeEmbeddedScripts(context, resources)
  }

  private fun configureContext(builder: PolyglotContextBuilder) {
    builder.disableOptions(
      "python.EmulateJython",
    )

    builder.enableOptions(
      "python.NativeModules",
      "python.LazyStrings",
      "python.WithTRegex",
    )

    if (ENABLE_EXPERIMENTAL) {
      builder.enableOptions("python.WithCachedSources")
      if (ENABLE_PANAMA) {
        builder.enableOptions("python.UsePanama")
      }
    }

    builder.setOptions(
      "python.HPyBackend" to "nfi",
      "python.PosixModuleBackend" to "java",
    )
    config.resourcesPath?.let {
      builder.setOptions(
        "python.HPyBackend" to "nfi",
        "python.PosixModuleBackend" to "native",
        "python.Sha3ModuleBackend" to "native",
      )

      builder.setOptions(
        "python.CoreHome" to "$it/python/python-home/lib/graalpy24.1",
        "python.SysPrefix" to "$it/python/python-home/lib/graalpy24.1",
        "python.StdLibHome" to "$it/python/python-home/lib/python3.11",
        "python.CAPI" to "$it/python/python-home/lib/graalpy24.1",
        "python.PythonHome" to "$it/python/python-home",
      )
    }

    config.executable?.let {
      builder.setOptions("python.Executable" to it)
    }
    config.executableList?.let {
      builder.setOptions("python.OrigArgv" to it.joinToString(GPY_LIST_SEPARATOR))
      builder.setOptions("python.ExecutableList" to it.joinToString(GPY_LIST_SEPARATOR))
    }
  }

  public companion object Plugin : AbstractLanguagePlugin<PythonConfig, Python>() {
    private const val PYTHON_LANGUAGE_ID = "python"
    private const val PYTHON_PLUGIN_ID = "Python"
    private const val ENABLE_EXPERIMENTAL = true
    private const val ENABLE_PANAMA = false
    private const val GPY_LIST_SEPARATOR = "🏆"
    override val languageId: String = PYTHON_LANGUAGE_ID
    override val key: Key<Python> = Key(PYTHON_PLUGIN_ID)

    init {
      registerLanguageVfs(PYTHON_LANGUAGE_ID) {
        object : LanguageVFSInfo {
          override val router: (Path) -> Boolean get() = { path ->
            path.toString().startsWith("<frozen ")
          }

          @Suppress("DEPRECATION")
          override val fsProvider: () -> FileSystem get() = {
            org.graalvm.python.embedding.utils.VirtualFileSystem
              .newBuilder()
              .build()
          }
        }
      }
    }

    override fun install(scope: InstallationScope, configuration: PythonConfig.() -> Unit): Python {
      configureLanguageSupport(scope)

      // apply the configuration and create the plugin instance
      val config = PythonConfig().apply(configuration)
      configureSharedBindings(scope, config)

      val resources = resolveEmbeddedManifest(scope)
      val instance = Python(config, resources)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)
      scope.lifecycle.on(ContextInitialized, instance::initializeContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)
      return instance
    }
  }
}
