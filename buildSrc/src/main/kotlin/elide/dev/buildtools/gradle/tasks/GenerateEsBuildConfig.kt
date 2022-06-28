package elide.dev.buildtools.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.io.File
import java.io.Serializable
import java.nio.charset.StandardCharsets


public val GenerateEsBuildConfig.outputBundleFile
  get() = File(outputBundleFolder, outputBundleName)

public val GenerateEsBuildConfig.outputPrepackedFile
  get() = File(outputBundleFolder, outputPrepackedName)

open class GenerateEsBuildConfig : DefaultTask() {
  enum class Mode {
    PRODUCTION, DEVELOPMENT
  }

  enum class Target {
    EMBEDDED, NODE, WEB
  }

  @get:Input
  var mode: Mode = Mode.DEVELOPMENT

  @get:Input
  var target: Target = Target.EMBEDDED

  @get:Input
  var libraryName: String = "embedded"

  @get:OutputFile
  var outputConfig by project.objects.property<File>()

  @get:OutputFile
  var processShim by project.objects.property<File>()

  @get:Input
  var outputBundleFolder by project.objects.property<String>()

  @get:Input
  var outputBundleName by project.objects.property<String>()

  @get:Input
  var outputPrepackedName by project.objects.property<String>()

  @get:InputFiles
  var modulesFolder = project.objects.listProperty(File::class)

  @get:Input
  var platform: String = "neutral"

  @get:Input
  var format: String = "iife"

  @get:Input
  var enableReact: Boolean = true

  @get:Input
  var minify: Boolean = false

  @get:Input
  var bundle: Boolean = true

  @get:InputFile
  var entryFile by project.objects.property<File>()

  init {
    with(project) {
      outputBundleFolder = file("$buildDir\\bundle").absolutePath
      outputBundleName = "bundle.js"
      outputPrepackedName = "bundle.pack.js"
      modulesFolder.set(listOf(
        file("node_modules"),
        file("${project.rootDir}/build/js/node_modules"),
      ).plus(if (enableReact) {
        listOf(file("${project(":packages:graalvm-react").projectDir}/src/main/node"))
      } else {
        emptyList()
      }))
    }
  }

  @get:Input
  val configTemplate = """
      const fs = require('fs');
      const esbuild = require('esbuild');
      const Prepack = require('prepack');
      const nodePath = process.env.NODE_PATH;
      if (!nodePath) {
        throw new Error("Failed to resolve NODE_PATH");
      }
      const settings = {
        entryPoints: ['%%%ENTRY%%%'],
        outfile: '%%%OUTFILE%%%',
        format: '%%%FORMAT%%%',
        minify: %%%MINIFY%%%,
        platform: '%%%PLATFORM%%%',
        globalName: '%%%LIBNAME%%%',
        bundle: %%%BUNDLE%%%,
        nodePaths: [%%%NODEPATH%%%],
        mainFields: ['module', 'main'],
        resolveExtensions: ['.ts','.js'],
        inject: [
          '%%%PROCESS%%%'
        ]
      };

      esbuild.build(
        settings
      ).catch(() => process.exit(1));

      const prepacked = Prepack.prepackFileSync([
        '%%%OUTFILE%%%'
      ], {
        compatibility: 'browser',
        inlineExpressions: true,
        timeout: 300 * 60 * 1000,
        sourceMaps: false,
        filename: 'ssr.js'
      });

      fs.writeFileSync('%%%PACKEDFILE%%%', prepacked.code, {
       encoding: 'utf8'
      });
    """.trimIndent()

  @get:Input
  val processShimTemplate = """
    export let process = {
      pid: -1,
      cwd: () => '',
      env: {},
      NODE_DEBUG: false,
      NODE_ENV: 'production',
      noDeprecation: false
    }
  """.trimIndent()

  private fun renderTemplateVals(tpl: String): String {
    return tpl.replace("%%%ENTRY%%%", entryFile.absolutePath.fixSlashes())
      .replace("%%%MODE%%%", mode.name.toLowerCase())
      .replace("%%%FORMAT%%%", format)
      .replace("%%%BUNDLE%%%", bundle.toString())
      .replace("%%%MINIFY%%%", minify.toString())
      .replace("%%%LIBNAME%%%", libraryName)
      .replace("%%%PLATFORM%%%", platform)
      .replace("%%%PACKEDFILE%%%", outputPrepackedFile.absolutePath.fixSlashes())
      .replace("%%%PROCESS%%%", processShim.absolutePath.fixSlashes())
      .replace("%%%OUTFILE%%%", outputBundleFile.absolutePath.fixSlashes())
      .replace("%%%NODEPATH%%%", modulesFolder.get().joinToString(",") {
        "'${it.absolutePath.fixSlashes()}'"
      })
  }

  @TaskAction
  fun buildFile() {
    processShim.writeText(
      renderTemplateVals(processShimTemplate)
    )
    outputConfig.writeText(
      renderTemplateVals(configTemplate)
    )
  }
}
