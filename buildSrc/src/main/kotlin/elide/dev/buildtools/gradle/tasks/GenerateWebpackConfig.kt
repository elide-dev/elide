package elide.dev.buildtools.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.io.File
import java.io.Serializable


val GenerateWebpackConfig.outputBundleFile
  get() = File(outputBundleFolder, outputBundleName)

fun String.appendIfMissing(s: String) =
  if (endsWith(s)) this else this + s


open class GenerateWebpackConfig : DefaultTask() {
  sealed class ResolveFallback : Serializable {
    abstract val moduleName: String

    data class ModuleFallback(
      override val moduleName: String,
      val resolveModuleName: String
    ) : ResolveFallback(), Serializable

    data class NoFallback(override val moduleName: String) : ResolveFallback(), Serializable
  }

  enum class Target {
    NODE, WEB
  }

  enum class Mode {
    PRODUCTION, DEVELOPMENT
  }

  data class TerserPluginSettings(
    var parallel: Boolean,
    var terserOptions: Options
  ) : Serializable {
    data class Options(
      val mangle: Boolean,
      val sourceMaps: Boolean,
      val keepClassnames: Regex,
      val keepFileNames: Regex
    ) : Serializable
  }

  private val template = """
        %%%IMPORTS%%%module.exports = [{
            mode: '%%%MODE%%%',
            name: 'server',
            entry: '%%%ENTRY%%%',
            target: '%%%Mode%%%',
            output: {
                path: '%%%OUTPUT_PATH%%%',
                filename: '%%%OUTPUT_NAME%%%',
                library: {
                    name: '%%%LIBRARY_NAME%%%',
                    type: '%%%LIBRARY_TYPE%%%'
                }
            },
            resolve: {
                modules: [%%%MODULES_FOLDER%%%]%%%FALLBACKS%%%
            }%%%MINIMIZER%%%
        }];
    """.trimIndent()

  @get:InputFile
  var entryFile by project.objects.property<File>()

  @get:Input
  var target: Target = Target.NODE

  @get:Input
  var mode: Mode = Mode.DEVELOPMENT

  @get:Input
  var libraryName: String = "embedded"

  @get:Input
  var libraryType: String = "this"

  @get:Input
  var outputBundleFolder by project.objects.property<String>()

  @get:Input
  var outputBundleName by project.objects.property<String>()

  @get:InputFiles
  var modulesFolder = project.objects.listProperty(File::class)

  @get:OutputFile
  var outputConfig by project.objects.property<File>()

  @get:Input
  var fallbacks = project.objects.listProperty(ResolveFallback::class)

  @get:Input
  @get:Optional
  val terserSettings = project.objects.property<TerserPluginSettings>()

  init {
    with(project) {
      outputBundleFolder = file("$buildDir\\bundle").absolutePath
      outputBundleName = "bundle.js"
      modulesFolder.set(listOf(file("node_modules")))
      outputConfig = file("$buildDir/config/webpack.config.js")
      fallbacks.set(emptyList())
    }
  }

  @TaskAction
  fun buildFile() {
    outputConfig.writeText(
      template.replace("%%%ENTRY%%%", entryFile.absolutePath.fixSlashes())
        .replace("%%%IMPORTS%%%", buildString {
          if (terserSettings.isPresent)
            appendLine("const TerserPlugin = require('terser-webpack-plugin');")
          appendLine()
        })
        .replace("%%%Mode%%%", target.name.toLowerCase())
        .replace("%%%OUTPUT_PATH%%%", outputBundleFolder.fixSlashes())
        .replace("%%%OUTPUT_NAME%%%", outputBundleName.appendIfMissing(".js"))
        .replace("%%%LIBRARY_NAME%%%", libraryName)
        .replace("%%%LIBRARY_TYPE%%%", libraryType)
        .replace("%%%MODE%%%", mode.name.toLowerCase())
        .replace(
          "%%%MODULES_FOLDER%%%",
          modulesFolder.get().joinToString(",") { "'${it.absolutePath.fixSlashes()}'" }
        )
        .replace("%%%FALLBACKS%%%", buildString {
          if (fallbacks.get().isNotEmpty()) {
            appendLine(",")
            appendLine("                fallback: {")
          }
          fallbacks.get().forEachIndexed { index, f: ResolveFallback ->
            when (f) {
              is ResolveFallback.ModuleFallback ->
                append("            '${f.moduleName}': require.resolve('${f.resolveModuleName}')")
              is ResolveFallback.NoFallback ->
                append("            '${f.moduleName}': false")
            }
            if (index != fallbacks.get().lastIndex)
              append(",")
          }
          if (fallbacks.get().isNotEmpty())
            appendLine("                }")
        })
        .replace("%%%MINIMIZER%%%", buildString {
          terserSettings.takeIf { it.isPresent }?.get()?.apply {
            appendLine(",")
            appendLine("    optimization: {")
            appendLine("        minimizer: [")
            appendLine("            new TerserPlugin({")
            appendLine("                parallel: $parallel,")
            appendLine("                terserOptions: {")
            appendLine("                    mangle: ${terserOptions.mangle},")
            appendLine("                    keep_classnames: new RegExp('${terserOptions.keepClassnames.pattern.fixSlashes()}'),")
            appendLine("                    keep_fnames: new RegExp('${terserOptions.keepFileNames.pattern.fixSlashes()}')")
            appendLine("                }")
            appendLine("            })")
            appendLine("        ]")
            appendLine("    }")
          }
        })
    )
  }
}

fun StringBuilder.appendLine(element: String = ""): StringBuilder =
  append(element).append("\n")

internal fun String.fixSlashes() =
  replace("\\", "\\\\")
