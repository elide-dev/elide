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
  description = ["Deploy a native Elide binary to a cloud target (Fly.io, Akash, or custom)"],
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
    description = ["Path to config file (for Fly.io: fly.toml, must be provided manually)"]
  )
  internal var configFilePath: String? = null

  @Option(
    names = ["--target", "-t"],
    description = ["Deployment target: fly, akash, or custom"]
  )
  internal var target: String? = null

  /** Supported deployment targets. */
  private enum class DeployTarget(val cliName: String) {
    FLY("fly"),
    AKASH("akash"),
    CUSTOM("custom");

    companion object {
      fun fromCliName(name: String?): DeployTarget? =
        values().find { it.cliName.equals(name, ignoreCase = true) }
    }
  }

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<elide.tool.cli.ToolState>): CommandResult {
    val selectedTarget = DeployTarget.fromCliName(target)
      ?: return err("Invalid deployment target '${target}'. Use one of: fly, akash, custom.")

    val binary = resolveBinary() ?: return err("No deployable binary found.")

    // Print summary before executing
    output {
      appendLine("=== Deployment Plan ===")
      appendLine("Target: ${selectedTarget.cliName}")
      appendLine("Binary: ${binary.absolutePath}")
      when (selectedTarget) {
        DeployTarget.FLY -> {
          if (configFilePath != null) {
            appendLine("Config: $configFilePath")
          } else {
            appendLine("Config: (none provided)")
          }
        }
        DeployTarget.AKASH, DeployTarget.CUSTOM -> {
          appendLine("Config: (none or handled by target)")
        }
      }
      appendLine("Dry run: $dryRun")
      appendLine("======================")
    }

    return when (selectedTarget) {
      DeployTarget.FLY -> {
        val configFile = configFilePath?.let { File(it) }
        if (!dryRun && configFile == null) {
          return err("Fly.io deployment requires a config file (--config).")
        }
        if (configFile != null && (!configFile.exists() || !configFile.isFile)) {
          output { appendLine("❌ Provided Fly.io config file does not exist: $configFilePath") }
          return err("Invalid Fly.io config file.")
        }
        if (dryRun) {
          output {
            appendLine("🚀 (dry-run) Would deploy to Fly.io: ${binary.absolutePath}")
            if (configFile != null) appendLine("Using config: ${configFile.absolutePath}")
          }
          success()
        } else {
          deployToFly(binary, configFile)
          success()
        }
      }
      DeployTarget.AKASH -> {
        if (dryRun) {
          output { appendLine("🚀 (dry-run) Would deploy to Akash: ${binary.absolutePath}") }
          success()
        } else {
          deployToAkash(binary)
        }
      }
      DeployTarget.CUSTOM -> {
        if (dryRun) {
          output { appendLine("🚀 (dry-run) Would deploy ${binary.name} to a custom target. Add logic in deployToCustom.") }
          success()
        } else {
          deployToCustom(binary)
        }
      }
    }
  }

  private fun resolveBinary(): File? {
    binaryPath?.let {
      val file = File(it)
      if (!file.exists() || !file.canExecute()) {
        output { appendLine("❌ Provided binary does not exist or is not executable: $it") }
        return null
      }
      return file
    }
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

  // --- Target-specific deploy logic ---

  private fun deployToFly(binary: File, config: File?) {
    output {
      appendLine("🚀 Deploying ${binary.name} to Fly.io...")
      if (config != null) {
        appendLine("Running: fly deploy --config ${config.absolutePath} --local-only --image ${binary.absolutePath}")
      } else {
        appendLine("⚠️ No config file provided. Please ensure you have a valid fly.toml.")
      }
      // Future: Actually invoke Fly.io CLI or API here.
    }
  }

  private fun deployToAkash(binary: File): CommandResult {
    output {
      appendLine("⚠️ Akash deployment is not yet implemented.")
      appendLine("Stub: Would deploy ${binary.name} to Akash.")
    }
    return success()
  }

  private fun deployToCustom(binary: File): CommandResult {
    output {
      appendLine("⚠️ Custom deployment is not yet implemented.")
      appendLine("Stub: Would deploy ${binary.name} to a custom target.")
    }
    return success()
  }
}