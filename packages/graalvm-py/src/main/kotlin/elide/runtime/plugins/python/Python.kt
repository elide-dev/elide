/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

import com.oracle.graal.python.PythonLanguage
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.io.FileSystem
import org.graalvm.python.embedding.GraalPythonFilesystem
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.ContextFinalized
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.extensions.disableOptions
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.extensions.setOptions
import elide.runtime.lang.python.PythonLang
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest
import elide.runtime.plugins.python.flask.FlaskAPI
import elide.runtime.vfs.LanguageVFS.LanguageVFSInfo
import elide.runtime.vfs.registerLanguageVfs

// Whether to enable the Python VFS.
private const val ENABLE_PYTHON_VFS = false

@DelicateElideApi public class Python(
  private val config: PythonConfig,
  private val scope: InstallationScope,
  @Suppress("unused") private val resources: LanguagePluginManifest? = null,
) {
  private fun initializeContext(context: PolyglotContext) {
    // apply init-time settings
    config.applyTo(context)
  }

  private fun finalizeContext(context: PolyglotContext) {
    // special case: inject flask
    // @TODO don't do this
    context.enter()
    try {
      when (val flask = scope.beanContext.getBean<FlaskAPI>(FlaskAPI::class.java)) {
        null -> logging.warn { "Failed to load Flask intrinsic" }
        else -> context.unwrap().polyglotBindings.putMember(
          // @TODO don't do this either
          // results in:
          // `__Elide_FlaskIntrinsic__`
          arrayOf("", "", "Elide", FlaskAPI.FLASK_INTRINSIC, "", "").joinToString("_"),
          flask,
        )
      }

      scope.deferred {
        context.evaluate(pythonInitSrc)
      }
    } finally {
      context.leave()
    }
  }

  private fun resolveGraalPythonVersions(): Pair<String, String> {
    val langVersion = PythonLanguage.VERSION
    val graalvmMajor = PythonLanguage.GRAALVM_MAJOR
    val graalvmMinor = PythonLanguage.GRAALVM_MINOR

    // `3.11.2` → `3.11`
    val parsedLangVersion = langVersion.split(".").slice(0..1).joinToString(".")
    return parsedLangVersion to "$graalvmMajor.$graalvmMinor"
  }

  private fun renderPythonPath(): String = sequence {
    System.getenv("VIRTUAL_ENV")?.let {
      yield(Path(it).resolve(SITE_PACKAGES_PATH).absolutePathString())
    }
    yieldAll(config.additionalPythonPaths)
  }.joinToString(":")

  private fun configureContext(builder: PolyglotContextBuilder) {
    builder.disableOptions(
      "python.EmulateJython",
      "python.DontWriteBytecodeFlag",
    )

    builder.enableOptions(
      "python.IsolateNativeModules",
      "python.LazyStrings",
      "python.WithTRegex",
      "python.NoSiteFlag", // @TODO
      "python.NoUserSiteFlag", // @TODO
      "python.PythonOptimizeFlag", // @TODO
    )

    if (ENABLE_EXPERIMENTAL) {
      builder.enableOptions("python.WithCachedSources")
      if (ENABLE_PANAMA) {
        builder.enableOptions("python.UsePanama")
      }
    }
    val engine = when (config.pythonEngine) {
      "default",
      "java" -> "java"
      "native" -> "native"
      else -> error("Unsupported Python engine: ${config.pythonEngine}")
    }
    builder.setOptions(
      "python.PosixModuleBackend" to engine,
      "python.PythonPath" to renderPythonPath(),
    )
    config.resourcesPath?.let {
      if (ImageInfo.inImageCode()) {
        val (pythonVersion, graalPyVersion) = resolveGraalPythonVersions()

        builder.setOptions(
          "python.CoreHome" to "$it/python/python-home/lib/graalpy$graalPyVersion",
          "python.SysPrefix" to "$it/python/python-home/lib/graalpy$graalPyVersion",
          "python.StdLibHome" to "$it/python/python-home/lib/python$pythonVersion",
          "python.CAPI" to "$it/python/python-home/lib/graalpy$graalPyVersion",
          "python.PythonHome" to "$it/python/python-home",
        )
      }
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
    private const val ENABLE_PANAMA = true
    private const val GPY_LIST_SEPARATOR = "🏆"
    private const val SITE_PACKAGES_PATH = "lib/python3.10/site-packages"
    override val languageId: String = PYTHON_LANGUAGE_ID
    override val key: Key<Python> = Key(PYTHON_PLUGIN_ID)

    private val logging by lazy {
      Logging.of(Python::class)
    }

    @JvmStatic private val pythonInitSrc: Source = requireNotNull(
      PythonLang::class.java.classLoader.getResourceAsStream("META-INF/elide/embedded/runtime/python/init.py")
    ) {
      "Failed to locate `init.py`; please check the classpath"
    }.bufferedReader(StandardCharsets.UTF_8).use {
      it.readText().let {
        Source.newBuilder(PYTHON_LANGUAGE_ID, it, "<elide>/init.py")
          .name("init.py")
          .cached(true)
          .internal(true)
          .interactive(false)
          .build()
      }
    }

    override fun install(scope: InstallationScope, configuration: PythonConfig.() -> Unit): Python {
      configureLanguageSupport(scope)

      // apply the configuration and create the plugin instance
      val config = PythonConfig().apply(configuration)
      configureSharedBindings(scope, config)

      val resources = resolveEmbeddedManifest(scope)
      val instance = Python(config, scope, resources)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)
      scope.lifecycle.on(ContextInitialized, instance::initializeContext)
      scope.lifecycle.on(ContextFinalized, instance::finalizeContext)

      if (ENABLE_PYTHON_VFS) registerLanguageVfs(PYTHON_LANGUAGE_ID) {
        object : LanguageVFSInfo {
          override val router: (Path) -> Boolean get() = { path ->
            val str = path.toString()
            str.startsWith("<frozen ") || str.contains("/python-home/")
          }

          override val fsProvider: () -> FileSystem get() = {
            GraalPythonFilesystem.delegate()
          }
        }
      }

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)
      return instance
    }
  }
}
