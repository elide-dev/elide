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

package elide.tool.cli.cmd.update

import com.vdurmont.semver4j.Semver
import lukfor.progress.Components
import lukfor.progress.TaskService
import lukfor.progress.tasks.DownloadTask
import lukfor.progress.tasks.ITaskRunnable
import lukfor.progress.tasks.TaskFailureStrategy.CANCEL_TASKS
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.IOUtils
import org.graalvm.nativeimage.ImageInfo
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GitHub
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrNull
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.HostPlatform
import elide.runtime.core.HostPlatform.OperatingSystem.WINDOWS
import elide.tool.cli.*
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.ToolState

/** Perform an update of Elide itself, swapping the binary for a new version (if one is available). */
@Command(
  name = "update",
  aliases = ["selfupdate"],
  description = ["%nUpdate Elide to the latest version"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
)
@Singleton
internal class SelfUpdateCommand : AbstractSubcommand<ToolState, CommandContext>() {
  companion object {
    private const val elideRespository = "elide-dev/releases"

    private fun String.parseElideVersion(): Semver = when {
      // like: 1.0-dev-19580
      contains("-dev-") -> {
        Semver("${substringBefore("-dev")}.0+${substringAfter("-dev-")}")
      }

      // like: 1.0-v3-alpha3-b1
      contains("-v3-") && contains("-alpha") -> {
        val alphaNum = split("-")[2].drop("alpha".length).toIntOrNull() ?: error(
          "Failed to parse alpha version"
        )
        Semver("${substringBefore("-")}.$alphaNum+${substringAfter("-")}")
      }

      // otherwise just parse
      else -> Semver(this)
    }
  }

  /** Dry-run; don't actually update. */
  @Option(
    names = ["--dry", "--dry-run"],
    description = ["Dry-run (don't actually update)"],
    defaultValue = "false",
  )
  var dry: Boolean = false

  /** Force an update from JVM when testing. */
  @Option(
    names = ["--force"],
    description = ["Force an update (testing only)"],
    hidden = true,
  )
  var force: Boolean = false

  /** Binary to update (during testing). */
  @Option(
    names = ["--target"],
    description = ["Binary target to update; defaults to self (testing only)."],
    hidden = true,
  )
  var target: String? = null

  /** Custom repository for updates. */
  @Option(
    names = ["--repository"],
    description = ["Custom update repository"],
  )
  var customRepository: String? = null

  private fun obtainActiveVersion(): Semver {
    // we either need to use *our* version (in prod), or the `target` version (under test)
    return ElideTool.version().parseElideVersion()
  }

  private fun githubClient(): GitHub = when (val token = System.getenv("GITHUB_TOKEN")) {
    null -> GitHub.connectAnonymously()
    else -> GitHub.connectUsingOAuth(token)
  }

  private fun obtainLatestVersion(): Pair<Semver, GHRelease> {
    return githubClient()
      .getRepository(customRepository ?: elideRespository)
      .listReleases()
      .first().let {
        // map to parsed version
        it.tagName.parseElideVersion() to it
      }
  }

  private fun resolveTarget(target: String): File = Path(target).toAbsolutePath().let { path ->
    when (path.isWritable()) {
      true -> path.toFile()
      else -> error(
        "Cannot self-update: target file '$path' is not writable"
      )
    }
  }

  @OptIn(DelicateElideApi::class)
  private fun selectDownload(release: GHRelease, platform: HostPlatform): URL {
    val osName = platform.os.name.lowercase()
    val archName = platform.arch.name.lowercase().replace("arm64", "aarch64")
    val tag = "$osName-$archName"
    val eligibleExtensions = when {
      platform.os == WINDOWS -> "zip"
      else -> "gz"
    }
    val asset = release.listAssets().find {
      it.name.contains(tag) && eligibleExtensions.any { ext -> it.name.endsWith(ext) }
    } ?: error(
      "No suitable release asset for version '${release.tagName}' (platform: '$tag')"
    )
    return URI.create(asset.browserDownloadUrl).toURL()
  }

  private fun unpackFile(archive: ZipFile, entry: ZipArchiveEntry, dest: File) {
    if (entry.isDirectory) {
      dest.resolve(entry.name).mkdirs()
    } else if (archive.canReadEntryData(entry)) {
      dest.resolve(entry.name).outputStream().use { out ->
        archive.getInputStream(entry).use { `in` ->
          IOUtils.copy(`in`, out)
        }
      }
    }
  }

  private fun unpackArchive(ext: String, archive: File, dest: File) {
    when (ext.trim().lowercase()) {
      "zip" -> ZipFile(archive).use {
        val entries = it.entries.toList()

        entries.stream().forEach { entry ->
          unpackFile(it, entry, dest)
        }
      }

      "gz" -> GZIPInputStream(archive.inputStream()).use { `in` ->
        dest.resolve("elide.bin").outputStream().use { out ->
          IOUtils.copy(`in`, out)
        }
      }

      else -> error("Unrecognized archive type: $ext")
    }
  }

  @OptIn(DelicateElideApi::class)
  private suspend fun CommandContext.performUpdate(
    release: GHRelease,
    target: File,
    latestVersion: String,
  ): CommandResult = withContext(Dispatchers.IO) {
    // resolve host platform, download URL, and temp target directory
    val platform = HostPlatform.resolve()
    logging.debug("Host platform: $platform")
    val downloadUrl = selectDownload(release, platform)
    logging.debug("Download URL: $downloadUrl")
    val ext = downloadUrl.toString().substringAfterLast(".")
    val tempDir = Files.createTempDirectory("elide-update").toFile()
    logging.debug("Destination dir: ${tempDir.toPath().absolute()}")

    val downloadedUpdate = tempDir.resolve("elide-update.$ext")
    val extractedUpdate = tempDir.resolve("elide.bin")
    val targetPath = target.toPath().toAbsolutePath()
    logging.debug("Target path: $targetPath")
    val targetBackupFile = target.parentFile.resolve("elide.old")
    val backupFilePath = targetBackupFile.toPath().toAbsolutePath()
    val extractedUpdatePath = extractedUpdate.toPath().toAbsolutePath()

    TaskService.setThreads(1)
    TaskService.setFailureStrategy(CANCEL_TASKS)
    TaskService.setAnimated(pretty)
    TaskService.setAnsiColors(pretty)
    TaskService.setTarget(System.out)

    logging.debug("Beginning download of update...")
    DownloadTask(downloadUrl, downloadedUpdate).let { downloadTask ->
      // monitor with spinner
      TaskService.monitor(Components.SPINNER, Components.DOWNLOAD).run(
        downloadTask,
      )
    }

    val failed = AtomicBoolean(false)
    TaskService.run(ITaskRunnable {
      try {
        unpackArchive(ext, downloadedUpdate, tempDir)
        if (targetBackupFile.exists()) {
          Files.delete(backupFilePath)
        }
        Files.move(targetPath, backupFilePath)
        Files.copy(extractedUpdatePath, targetPath)
        if (!Files.isExecutable(targetPath)) {
          targetPath.toFile().setExecutable(true)
        }
      } catch (err: Throwable) {
        logging.error("Update failed: ${err.message}")
        failed.compareAndSet(false, true)
      }
    })
    output {
      if (failed.get()) {
        appendLine("⨯ Update failed. Please examine the logs and report this via `elide bug`.")
      } else {
        appendLine("✔ Update complete → $latestVersion")
      }
    }
    success()
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val cmdPath = ProcessHandle.current().info().command().getOrNull() ?: error(
      "Failed to resolve command path; please report this with `elide bug`"
    )
    output {
      appendLine("Updating Elide...")
    }
    val target = when {
      // if we are running in test mode...
      target != null && force -> target!!  // bypass

      // otherwise, we should fail (gracefully) because we aren't in a JVM
      !ImageInfo.inImageCode() || cmdPath.endsWith("/java") -> {
        output {
          appendLine("Cannot self-update Elide: running in JVM mode (please use Maven)")
        }
        return success()
      }

      // we're running on native
      else -> cmdPath
    }
    logging.debug("Update target: '$target'")

    val latest: AtomicReference<Semver> = AtomicReference()
    val version: AtomicReference<Semver> = AtomicReference()
    val release: AtomicReference<GHRelease> = AtomicReference()

    output {
      appendLine("Checking for latest release..")
    }

    logging.debug("Checking GitHub for latest release...")
    val (latestVersion, ghRelease) = obtainLatestVersion()
    val currentVersion = obtainActiveVersion()
    latest.set(latestVersion)
    version.set(currentVersion)
    release.set(ghRelease)
    val tag = ghRelease.tagName
    logging.debug("Latest release is '$tag'")

    output {
      appendLine("Latest release: $latest")
      appendLine("Your release: $version")

      when {
        // dev versions can't update manually
        !force && ElideTool.version().contains("-dev-") -> {
          appendLine("You are using a development copy of Elide ($version); update aborted.")
        }

        // dry update bails early
        dry -> {
          appendLine("Would proceed with update ($version → $latest), but `dry` was specified; exiting.")
        }
      }
    }
    return if (!dry) try {
      logging.debug("Initiating binary update")

      performUpdate(
        release.get(),
        resolveTarget(target),
        latest.get().value,
      )
      success()
    } catch (err: Throwable) {
      err("Update failed: ${err.message}")
    } else {
      logging.debug("Dry run; exiting before update.")
      success()
    }
  }
}
