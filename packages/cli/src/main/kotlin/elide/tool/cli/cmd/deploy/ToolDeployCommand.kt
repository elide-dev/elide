package dev.elide.tool.cli.cmd.deploy

import dev.elide.tool.cli.ProjectAwareSubcommand
import dev.elide.tool.cli.CommandContext
import dev.elide.tool.cli.ToolContext
import dev.elide.tool.cli.CommandResult
import dev.elide.tool.cli.err
import dev.elide.tool.cli.success
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import kotlin.io.path.writeText

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
    names = ["--config"],
    description = ["Target-specific config file, if applicable"]
  )
  internal var configPath: String? = null

  @Option(
    names = ["--target", "-t"],
    description = ["Deployment target: fly, akash, or custom"]
  )
  internal var target: String? = null

  @Option(
    names = ["--image-tag"],
    description = ["Docker image tag to use (default: elide-app:latest)"]
  )
  internal var imageTag: String = "elide-app:latest"

  /** Supported deployment targets. */
  private enum class DeployTarget(val cliName: String, val requiresConfig: Boolean = false) {
    FLY("fly", requiresConfig = true),
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
      if (configPath != null) {
        appendLine("Target config: $configPath")
      } else {
        appendLine("Target config: (none provided)")
      }
      appendLine("Image tag: $imageTag")
      appendLine("Dry run: $dryRun")
      appendLine("======================")
    }

    // Build Docker image
    val imageBuildResult = buildDockerImage(binary, imageTag, dryRun)
    if (!imageBuildResult) {
      return err("Failed to build Docker image.")
    }

    return when (selectedTarget) {
      DeployTarget.FLY -> deployToFly(binary, configPath, imageTag, dryRun)
      DeployTarget.AKASH -> deployToAkash(imageTag, dryRun)
      DeployTarget.CUSTOM -> {
        if (dryRun) {
          output { appendLine("🚀 (dry-run) Would deploy Docker image $imageTag to a custom target. Add logic in deployToCustom.") }
          success()
        } else {
          deployToCustom(imageTag)
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

  /**
   * Build a minimal Docker image from the binary.
   * Always cleans up the temp directory.
   */
  private fun buildDockerImage(binary: File, tag: String, dryRun: Boolean): Boolean {
    val dockerfileContent = """
      FROM scratch
      COPY ${binary.name} /app
      ENTRYPOINT ["/app"]
    """.trimIndent()

    val tempDir = Files.createTempDirectory("elide-docker-build")
    try {
      val dockerfilePath = tempDir.resolve("Dockerfile")
      val binaryTarget = tempDir.resolve(binary.name)
      dockerfilePath.writeText(dockerfileContent)
      binary.copyTo(binaryTarget.toFile(), overwrite = true)

      val buildCmd = "docker build -t $tag ${tempDir.toAbsolutePath()}"
      if (dryRun) {
        output { appendLine("🧱 (dry-run) Would run: $buildCmd") }
        return true
      }

      val process = ProcessBuilder("sh", "-c", buildCmd)
        .inheritIO()
        .start()
      val exitCode = process.waitFor()
      if (exitCode != 0) {
        output { appendLine("❌ Docker build failed with exit code $exitCode") }
        return false
      }
      output { appendLine("🧱 Built Docker image: $tag") }
      return true
    } catch (e: Exception) {
      output { appendLine("❌ Failed to prepare Docker build context: ${e.message}") }
      return false
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }

  // --- Target-specific deploy logic ---

  private fun deployToFly(binary: File, configPath: String?, imageTag: String, dryRun: Boolean): CommandResult {
    val configFile = configPath?.let { File(it) }
    if (!dryRun && configFile == null) {
      return err("This target requires a config file (--config).")
    }
    if (configFile != null && (!configFile.exists() || !configFile.isFile)) {
      output { appendLine("❌ Provided config file does not exist: $configPath") }
      return err("Invalid config file for target.")
    }
    // Optional: Warn if fly.toml [build] section has an image set that conflicts with --image-tag
    if (configFile != null) {
      val configText = configFile.readText()
      val buildImageRegex = Regex("""^\s*image\s*=\s*["'][^"']+["']""", RegexOption.MULTILINE)
      if (buildImageRegex.containsMatchIn(configText)) {
        output {
          appendLine("⚠️ Warning: Your config file sets a [build] image. This may conflict with --image-tag and the built image.")
        }
      }
    }
    val deployCmd = buildString {
      append("fly deploy --local-only --image $imageTag")
      if (configFile != null) append(" --config ${configFile.absolutePath}")
    }
    if (dryRun) {
      output {
        appendLine("🚀 (dry-run) Would deploy to Fly.io using:")
        appendLine(deployCmd)
      }
      return success()
    } else {
      output { appendLine("🚀 Deploying Docker image $imageTag to Fly.io...") }
      val process = ProcessBuilder("sh", "-c", deployCmd)
        .inheritIO()
        .start()
      val exitCode = process.waitFor()
      if (exitCode != 0) {
        output { appendLine("❌ Fly.io deploy failed with exit code $exitCode") }
        return err("Fly.io deploy failed.")
      }
      output { appendLine("✅ Fly.io deploy succeeded.") }
      return success()
    }
  }

  private fun deployToAkash(imageTag: String, dryRun: Boolean): CommandResult {
    if (dryRun) {
      output {
        appendLine("🚀 (dry-run) Would push image $imageTag to registry and submit Akash deployment.")
      }
      return success()
    }
    // TODO: Add support for pushing $imageTag to remote registry for Akash
    output {
      appendLine("⚠️ Akash deployment is not yet implemented.")
      appendLine("Stub: Would push image $imageTag to registry and submit Akash deployment.")
    }
    return success()
  }

  private fun deployToCustom(imageTag: String): CommandResult {
    output {
      appendLine("⚠️ Custom deployment is not yet implemented.")
      appendLine("Stub: Would deploy Docker image $imageTag to a custom target.")
    }
    return success()
  }
}