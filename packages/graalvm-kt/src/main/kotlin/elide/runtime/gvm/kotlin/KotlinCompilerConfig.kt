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
package elide.runtime.gvm.kotlin

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import java.nio.file.Path
import java.util.EnumSet
import kotlin.collections.orEmpty
import kotlin.collections.toMutableList
import kotlin.io.path.absolutePathString
import elide.runtime.precompiler.Precompiler
import elide.tooling.jvm.JvmLibraries

// Constant plugin names.
private const val SERIALIZATION_PLUGIN_NAME = "kotlin-serialization-compiler-plugin-embeddable"
private const val POWERASSERT_PLUGIN_NAME = "kotlin-power-assert-compiler-plugin-embeddable"
private const val REDACTED_PLUGIN_NAME = "redacted-compiler-plugin"

/**
 * Configures the Kotlin compiler which is embedded within Elide.
 *
 * @property apiVersion The API version of the Kotlin compiler.
 * @property languageVersion The language version of the Kotlin compiler.
 * @property plugins String plugin specifications to pass to the Kotlin compiler.
 * @property testMode Whether this configuration is for test mode, which may enable additional plugins.
 * @property builtinPlugins Set of built-in plugins to enable for the Kotlin compiler.
 *
 * @see KotlinBuiltinPlugin known built-in plugins
 */
public data class KotlinCompilerConfig(
  public val apiVersion: ApiVersion,
  public val languageVersion: LanguageVersion,
  public val plugins: List<KotlinPluginConfig> = emptyList(),
  public val testMode: Boolean = false,
  public val builtinPlugins: Set<KotlinBuiltinPlugin> = if (testMode) DEFAULT_PLUGINS_TEST else DEFAULT_PLUGINS,
) : Precompiler.Configuration {
  /**
   * Configures a Kotlin compiler plugin.
   */
  public interface KotlinPluginConfig {
    public fun apply(args: K2JVMCompilerArguments, root: Path)
  }

  /**
   * Defines known built-in plugins for the Kotlin compiler.
   */
  @Suppress("MaxLineLength")
  public enum class KotlinBuiltinPlugin : KotlinPluginConfig {
    SERIALIZATION {
      override fun apply(args: K2JVMCompilerArguments, root: Path) {
        val artifact = root
          .resolve("kotlin")
          .resolve(KotlinLanguage.VERSION)
          .resolve("lib")
          .resolve("$SERIALIZATION_PLUGIN_NAME-${KotlinLanguage.VERSION}.jar")

        initializePlugin(
          args = args,
          artifact = artifact,
        )
      }
    },

    POWER_ASSERT {
      override fun apply(args: K2JVMCompilerArguments, root: Path) {
        val artifact = root
          .resolve("kotlin")
          .resolve(KotlinLanguage.VERSION)
          .resolve("lib")
          .resolve("$POWERASSERT_PLUGIN_NAME-${KotlinLanguage.VERSION}.jar")

        initializePlugin(
          args = args,
          artifact = artifact,
          options = powerAssertSymbols.map { "function" to it }.toList(),
        )
      }
    },

    REDACTED {
      override fun apply(args: K2JVMCompilerArguments, root: Path) {
        val artifact = root
          .resolve("kotlin")
          .resolve(KotlinLanguage.VERSION)
          .resolve("lib")
          .resolve("$REDACTED_PLUGIN_NAME-${JvmLibraries.EMBEDDED_REDACTED_VERSION}.jar")

        initializePlugin(
          args = args,
          artifact = artifact,
          options = buildList {
            add("enabled" to "true")
            add("replacementString" to "██")
            add("unredactedAnnotations" to unredactedAnnos.joinToString(",") { it.replace(".", "/") })
            add("redactedAnnotations" to redactedAnnos.joinToString(",") { it.replace(".", "/") })
          },
        )
      }
    };

    private companion object {
      @JvmStatic private fun initializePlugin(
        args: K2JVMCompilerArguments,
        artifact: Path,
        options: List<Pair<String, String>> = emptyList()
      ) {
        args.pluginConfigurations = (args
          .pluginConfigurations
          .orEmpty()
          .toMutableList() + buildString {
            append(artifact.absolutePathString())
            if (options.isNotEmpty()) {
              append("=")
              append(options.joinToString(",") { "${it.first}=${it.second}" })
            }
          }
        ).toTypedArray()
      }
    }
  }

  public companion object {
    /** Default Kotlin compiler configuration. */
    public val DEFAULT: KotlinCompilerConfig = KotlinPrecompiler.currentConfig()

    /** Default suite of Kotlin plugins to enable. */
    private val DEFAULT_PLUGINS: Set<KotlinBuiltinPlugin> = EnumSet.of(
      KotlinBuiltinPlugin.SERIALIZATION,
      KotlinBuiltinPlugin.REDACTED,
    )

    /** Default suite of Kotlin plugins to enable in test mode. */
    private val DEFAULT_PLUGINS_TEST: Set<KotlinBuiltinPlugin> = DEFAULT_PLUGINS + EnumSet.of(
      KotlinBuiltinPlugin.POWER_ASSERT,
    )

    @JvmStatic public fun getDefaultPlugins(test: Boolean = false): Set<KotlinBuiltinPlugin> {
      return if (test) DEFAULT_PLUGINS_TEST else DEFAULT_PLUGINS
    }

    private val redactedAnnos = sortedSetOf(
      "elide.annotations.Secret",
    )

    private val unredactedAnnos = sortedSetOf(
      "elide.annotations.Cleartext",
    )

    private val powerAssertSymbols = sortedSetOf(
      "kotlin.assert",
      "kotlin.test.assertEquals",
      "kotlin.test.assertNotEquals",
      "kotlin.test.assertTrue",
      "kotlin.test.assertFalse",
      "kotlin.test.assertNull",
      "kotlin.test.assertNotNull",
      "kotlin.test.assertContentEquals",
      "kotlin.test.assertContentSame",
      "kotlin.test.assertFailsWith",
      "kotlin.test.assertFails",
      "kotlin.test.assertIs",
    )
  }
}
