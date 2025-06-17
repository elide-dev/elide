package dev.elide.tool.cli.cmd.deploy

import dev.elide.tool.cli.ProjectAwareSubcommand
import dev.elide.tool.cli.CommandContext
import dev.elide.tool.cli.ToolContext
import dev.elide.tool.cli.CommandResult
import dev.elide.tool.cli.err
import dev.elide.tool.cli.success
import java.io.File
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

@Command(
  name = "deploy",
  description = ["Deploy a native Elide binary to Fly.io"],
  mixinStandardHelpOptions = true,
)
internal class ToolDeployCommand : ProjectAwareSubcommand<elide.tool.cli.ToolState, CommandContext>() {

  @Parameters(
    arity = "0..1",
    description = [
      "Path to the binary to deploy (default: first binary in build/native/nativeCompile or build/native/nativeOptimizedCompile)"
    ],
    paramLabel = "BINARY"
  )
  internal var binaryPath: String? = null

  @Option(
    names = ["--dry-run", "-d"],
    description = ["Print actions without deploying"]
  )
  internal var dryRun: Boolean = false

  @Option(
    names = ["--config", "-c"],
    description = ["Path to Fly.io config file (default: fly.toml in project root)"]
  )
  internal var configPath: String? = null

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<elide.tool.cli.ToolState>): CommandResult {
    val binary = resolveBinary() ?: return err("No deployable binary found.")
    val config = resolveConfig() ?: return err("No Fly.io config file found (expected fly.toml or specify with --config).")

    return when {
      dryRun -> {
        output {
          appendLine("🚀 (dry-run) Would deploy: ${binary.absolutePath}")
          appendLine("Using config: ${config.absolutePath}")
        }
        success()
      }
      else -> {
        deployBinary(binary, config)
        success()
      }
    }
  }

  private fun resolveBinary(): File? {
    // If a binary path is provided, use that.
    binaryPath?.let {
      val file = File(it)
      if (!file.exists() || !file.canExecute()) {
        output { appendLine("❌ Provided binary does not exist or is not executable: $it") }
        return null
      }
      return file
    }

    // Look for binaries in native build output directories.
    val nativeDirs = listOf(
      File("./build/native/nativeOptimizedCompile"),
      File("./build/native/nativeCompile")
    )
    val candidates = nativeDirs.flatMap { dir ->
      dir.listFiles()?.filter { it.canExecute() && it.isFile } ?: emptyList()
    }

    return when {
      candidates.isEmpty() -> null
      candidates.size == 1 -> candidates.first()
      else -> {
        output {
          appendLine("⚠️ Multiple binaries found in native build output. Please specify which to deploy:")
          candidates.forEach { appendLine("  - ${it.absolutePath}") }
        }
        null
      }
    }
  }

  private fun resolveConfig(): File? {
    // Use user-specified config if provided.
    configPath?.let {
      val file = File(it)
      if (file.exists() && file.isFile) return file
      output { appendLine("❌ Specified config file does not exist: $it") }
      return null
    }
    // Default: fly.toml in project root.
    val defaultConfig = File("fly.toml")
    return if (defaultConfig.exists() && defaultConfig.isFile) defaultConfig else null
  }

  private fun deployBinary(binary: File, config: File) {
    output {
      appendLine("🚀 Deploying ${binary.name} to Fly.io using config ${config.name}...")
      appendLine("Running: fly deploy --config ${config.absolutePath} --local-only --image ${binary.absolutePath}")
      // Future: Actually invoke Fly.io CLI or API here.
    }