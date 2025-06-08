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

import org.graalvm.nativeimage.ImageInfo
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import java.nio.file.Path
import java.util.EnumSet
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import elide.runtime.precompiler.Precompiler

// Name of the serialization plugin JAR file, which is expected to be present in the Kotlin resources.
private const val SERIALIZATION_PLUGIN_NAME = "kotlin-serialization-compiler-plugin-embeddable"

/**
 * Configures the Kotlin compiler which is embedded within Elide.
 *
 * @property apiVersion The API version of the Kotlin compiler.
 * @property languageVersion The language version of the Kotlin compiler.
 * @property plugins String plugin specifications to pass to the Kotlin compiler.
 * @property builtinPlugins Set of built-in plugins to enable for the Kotlin compiler.
 *
 * @see KotlinBuiltinPlugin known built-in plugins
 */
public data class KotlinCompilerConfig(
  public val apiVersion: ApiVersion,
  public val languageVersion: LanguageVersion,
  public val plugins: List<KotlinPluginConfig> = emptyList(),
  public val builtinPlugins: Set<KotlinBuiltinPlugin> = DEFAULT_PLUGINS,
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

        if (ImageInfo.inImageRuntimeCode()) {
          args.pluginConfigurations = (args
            .pluginConfigurations
            .orEmpty()
            .toMutableList() + artifact.absolutePathString())
            .toTypedArray()
          return
        }
        require(!(ImageInfo.inImageCode() && ImageInfo.inImageRuntimeCode())) {
          "Cannot load Kotlin serialization plugin in native image runtime mode (loads at build time)"
        }
        val kotlinSerializationPlugin = requireNotNull(
          System.getProperty("elide.kotlinResources")?.ifEmpty { null }?.ifBlank { null }
            ?.let { Path.of(it).resolve("lib").resolve("$SERIALIZATION_PLUGIN_NAME-${KotlinLanguage.VERSION}.jar") }
            ?.takeIf { it.exists() }
          ?: artifact.takeIf { it.exists() }
        ) {
          "Kotlin serialization plugin not found: $SERIALIZATION_PLUGIN_NAME-${KotlinLanguage.VERSION}.jar"
        }
        args.pluginConfigurations = (args
          .pluginConfigurations
          .orEmpty()
          .toMutableList() + kotlinSerializationPlugin.absolutePathString())
          .toTypedArray()
      }
    },
  }

  public companion object {
    /** Default Kotlin compiler configuration. */
    public val DEFAULT: KotlinCompilerConfig = KotlinPrecompiler.currentConfig()

    /** Default suite of Kotlin plugins to enable. */
    public val DEFAULT_PLUGINS: Set<KotlinBuiltinPlugin> = EnumSet.of(
      KotlinBuiltinPlugin.SERIALIZATION,
    )
  }
}
