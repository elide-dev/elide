package elide.manager

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.bouncycastle.crypto.digests.SHA256Digest
import org.graalvm.nativeimage.ImageInfo
import org.tukaani.xz.XZInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.runBlocking
import kotlinx.io.*
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource
import elide.annotations.Singleton
import elide.manager.repository.ElideRepository
import elide.manager.repository.ElideRepositoryFactory
import elide.manager.repository.getFile
import elide.runtime.core.HostPlatform

/**
 * Implementation of [InstallManager].
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class InstallManagerImpl(repositoryFactory: ElideRepositoryFactory) : InstallManager {
  private val config: ElideInstallConfig by lazy { resolveConfig() }
  private val repositories by lazy { config.repositories.asSequence().map { repositoryFactory.get(it) }.toList() }

  override fun onStartup(currentVersion: String, requestedVersion: String?): String? {
    val version = requestedVersion ?: readVersionFile() ?: return null
    if (version == currentVersion) return null
    val path =
      getInstallations(true).find { it.version.version == version }?.path ?: runBlocking { install(false, version) } ?: return null
    return resolveBinary(Path(path)).toString()
  }

  override fun getInstallations(includeSearchDirs: Boolean): List<ElideInstallation> =
    (if (includeSearchDirs) (config.searchDirs + config.installDirs).distinct() else config.installDirs)
      .asSequence()
      .map { Path(it) }
      .flatMap { if (SystemFileSystem.exists(it)) SystemFileSystem.list(it) else emptyList() }
      .flatMap { resolveInstall(it) }
      .toList()

  override fun getInstallPaths(): List<String> =
    (listOfNotNull(config.defaultInstallDir) + config.installDirs).distinct()

  override suspend fun getAvailable(onlyCurrentSystem: Boolean): List<ElideVersionDto> =
    repositories
      .flatMap { it.getVersions() }
      .let { if (onlyCurrentSystem) it.filter { ver -> ver.platform == PLATFORM } else it }

  override suspend fun install(elevated: Boolean, version: String, path: String?, progress: FlowCollector<ElideInstallEvent>?): String? {
    repositories.forEach { repository ->
      repository
        .getVersions()
        .find { it.version == version && it.platform == PLATFORM }
        ?.let {
          return install(elevated, repository, it, path, progress)
        }
    }
    println("Elide version $version not found in repositories")
    return null
  }

  override suspend fun verifyInstall(path: String, progress: FlowCollector<ElideFileVerifyEvent>?): List<String> {
    val path = Path(path)
    val stampFile = Path(path, STAMP_FILE)
    require(SystemFileSystem.exists(stampFile)) { "stampfile does not exist" }
    val lines = SystemFileSystem.source(stampFile).buffered().readString().trim().lines()
    progress?.emit(FileVerifyStartEvent)
    val failed = mutableListOf<String>()
    lines.forEachIndexed { index, line ->
      val (hash, relativePath) = line.split("  ./")
      progress?.emit(FileVerifyProgressEvent(index.toFloat() / lines.size.toFloat(), relativePath))
      val realHash = calculateHash(Path(path, relativePath))
      if (hash != realHash) { failed.add(relativePath) }
    }
    progress?.emit(FileVerifyCompletedEvent)
    return failed
  }

  override suspend fun uninstall(elevated: Boolean, installation: ElideInstallation, progress: FlowCollector<ElideUninstallEvent>?) {
    val installDir = Path(installation.path)
    val files = installDir.recursive()
    if (files.all {
      canWrite(it, true)
    }) {
      val size = files.count()
      progress?.emit(UninstallStartEvent)
      files.forEachIndexed { index, file ->
        progress?.emit(UninstallProgressEvent(index.toFloat() / size.toFloat(), file.toString().substringAfter(installation.path)))
        SystemFileSystem.delete(file)
      }
      progress?.emit(UninstallCompletedEvent)
    }
    else if (!elevated) elevatedUninstall(installation)
    else throw IllegalStateException("Process is already elevated but files cannot be deleted")
  }

  private fun Path.recursive(): Sequence<Path> = SystemFileSystem.list(this).asSequence().flatMap {
    if (SystemFileSystem.metadataOrNull(it)!!.isDirectory) {
      it.recursive()
    } else {
      sequenceOf(it)
    }
  } + this

  private fun resolveConfig(): ElideInstallConfig {
    val global = readConfig(GLOBAL_CONFIG_PATH)
    val user = readConfig(USER_CONFIG_PATH)
    return (global?.let { user?.appendDistinct(it) ?: it } ?: user)?.appendDistinct(DEFAULT_CONFIG) ?: DEFAULT_CONFIG
  }

  @OptIn(ExperimentalSerializationApi::class)
  private fun readConfig(path: String): ElideInstallConfig? =
    try {
      val path = Path(path)
      if (!SystemFileSystem.exists(path)) null
      else Json.Default.decodeFromSource(SystemFileSystem.source(path).buffered())
    } catch (_: Throwable) {
      null
    }

  private fun ElideInstallConfig.appendDistinct(other: ElideInstallConfig): ElideInstallConfig =
    copy(
      installDirs = installDirs + other.installDirs.filterNot { it in installDirs },
      repositories = repositories + other.repositories.filterNot { it in repositories },
      defaultInstallDir = defaultInstallDir ?: other.defaultInstallDir,
    )

  @OptIn(ExperimentalStdlibApi::class)
  private fun calculateHash(path: Path): String {
    val digest = SHA256Digest()
    val bytes = SystemFileSystem.source(path).buffered().readByteArray()
    digest.update(bytes, 0, bytes.size)
    val out = ByteArray(digest.digestSize)
    digest.doFinal(out, 0)
    return out.toHexString()
  }

  private fun readVersionFile(): String? =
    Path(System.getProperty("user.dir"), PROJECT_VERSION_FILE).let {
      if (SystemFileSystem.exists(it)) SystemFileSystem.source(it).buffered().readString(Charsets.UTF_8).trim()
      else null
    }

  private fun resolveInstall(path: Path): Sequence<ElideInstallation> {
    try {
      val files = SystemFileSystem.list(path)
      val version =
        files.find { it.name == VERSION_FILE }?.let { SystemFileSystem.source(it).buffered().readString().trim() }
          ?: resolveBinary(path)?.let {
            ProcessBuilder(it.toString(), "--version")
              .start()
              .inputStream
              .readAllBytes()
              .toString(Charsets.UTF_8)
              .trim()
          }
          ?: return emptySequence()
      return sequenceOf(ElideInstallation(ElideVersionDto(version, PLATFORM), path.toString()))
    } catch (_: Throwable) {
      return emptySequence()
    }
  }

  private fun resolveBinary(path: Path): Path? =
    SystemFileSystem.list(path)
      .find { it.name == BINARY_NAME || it.name == "bin" }
      ?.let {
        if (it.name == BINARY_NAME) it
        else Path(it, BINARY_NAME).let { elide -> if (SystemFileSystem.exists(elide)) it else null }
      }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun install(
    elevated: Boolean,
    repository: ElideRepository,
    version: ElideVersionDto,
    path: String?,
    progress: FlowCollector<ElideInstallEvent>? = null
  ): String? {
    val path = path ?: config.defaultInstallDir ?: DEFAULT_INSTALL_PATH
    val installDir = Path(path, version.version)
    if (SystemFileSystem.exists(installDir)) throw IllegalArgumentException("Directory $installDir already exists")
    if (!canWrite(installDir)) {
      if (!elevated) return elevatedInstall(version, path, installDir)
      else throw IllegalStateException("Process is already elevated but path $installDir cannot be created")
    }
    val buffer = Buffer()
    val format = "txz"
    repository.getFile(version, format, buffer, progress)
    val hash = repository.getFile(version, "$format.sha256").decodeToString().substringBefore(' ')
    if (!checkHash(buffer, hash.hexToByteArray(), progress)) throw IllegalArgumentException("invalid hash")

    SystemFileSystem.createDirectories(installDir, true)
    installXz(installDir, buffer, progress)
    val stampFile = Path(installDir, STAMP_FILE)
    if (SystemFileSystem.exists(stampFile)) {
      verifyInstall(installDir.toString(), progress).apply {
        require(isEmpty()) { "The following files were not installed correctly:\n${joinToString("\n")}" }
      }
    }
    else progress?.emit(FileVerifyIndeterminateEvent)

    return installDir.toString()
  }

  private fun canWrite(path: Path, tryChangePermissions: Boolean = false): Boolean {
    var path = path
    while (!SystemFileSystem.exists(path)) {
      path = path.parent ?: return false
    }
    val javaPath = Paths.get(path.toString())
    return if (Files.isWritable(javaPath)) true
    else if (tryChangePermissions) javaPath.toFile().setWritable(true, true)
    else false
  }

  private fun elevatedInstall(version: ElideVersionDto, path: String, installDir: Path): String? = if (elevatedAction(buildList {
      add("manager")
      add("--install-version")
      add(version.version)
      add("--install-path")
      add(path)
      add("--no-confirm")
      add("--elevated")
    })) installDir.toString() else null

  private fun elevatedUninstall(install: ElideInstallation): Boolean = elevatedAction(buildList {
    add("manager")
    add("--uninstall-version")
    add(install.version.version)
    add("--install-path")
    add(install.path)
    add("--no-confirm")
    add("--elevated")
  })

  private fun elevatedAction(params: List<String>): Boolean = if (ImageInfo.isExecutable() && runElevated(params) == 0) true
  else {
    println(
      "Please run this version of elide with the following parameters as root/administrator to proceed with installation")
    println(params.joinToString(" ") { it.escapeWhitespace("\"") })
    false
  }

  private fun runElevated(params: List<String>): Int {
    val path = Paths.get(ProcessHandle.current().info().command().get()).toAbsolutePath().toString()
    val pathAndParams = listOf(path) + params
    val process: ProcessBuilder =
      when (PLATFORM.os) {
        HostPlatform.OperatingSystem.LINUX ->
          ProcessBuilder("pkexec", *pathAndParams.map { it.escapeWhitespace("\"") }.toTypedArray()).inheritIO()
        HostPlatform.OperatingSystem.DARWIN -> {
          val escaped = pathAndParams.joinToString(" ") { it.escapeWhitespace("'") }
          ProcessBuilder("osascript", "-e", "do shell script \"$escaped\" with administrator privileges").inheritIO()
        }
        HostPlatform.OperatingSystem.WINDOWS -> ProcessBuilder(
          "powershell.exe",
          "-command",
          "Start-Process",
          "-Wait",
          "-FilePath",
          "\"\"\"$path\"\"\"",
          "-verb RunAs",
          "-ArgumentList",
          params.joinToString(" ") { it.escapeWhitespace("`\"") })
      }
    return process.start().waitFor()
  }

  private fun String.escapeWhitespace(escape: String): String = if (contains(' ')) "$escape$this$escape" else this

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun checkHash(
    source: Buffer,
    hash: ByteArray,
    progress: FlowCollector<ElideInstallEvent>? = null
  ): Boolean {
    val size = source.size
    val source = source.peek()
    val digest = SHA256Digest()
    var read = 0L
    progress?.emit(VerifyStartEvent)
    var progressCounter = 0
    while (!source.exhausted()) {
      if (progressCounter == PROGRESS_INTERVAL) {
        progress?.emit(VerifyProgressEvent(read.toFloat() / size.toFloat()))
        progressCounter = 0
      }
      val data = Buffer()
      read += source.readAtMostTo(data, BUFFER)
      val arr = data.readByteArray()
      digest.update(arr, 0, arr.size)
      progressCounter++
    }
    val out = ByteArray(digest.digestSize)
    digest.doFinal(out, 0)
    progress?.emit(VerifyCompletedEvent)
    return out.contentEquals(hash)
  }

  private suspend fun installXz(installDir: Path, buffer: Buffer, progress: FlowCollector<ElideInstallEvent>?) {
    val size = buffer.size
    val posix = PLATFORM.os != HostPlatform.OperatingSystem.WINDOWS
    progress?.emit(InstallStartEvent)
    XZInputStream(buffer.asInputStream()).use {
      TarArchiveInputStream(it).use { stream ->
        stream.asSource().buffered().use { source ->
          for (entry in stream) {
            val realName = entry.name.substringAfter('/')
            if (realName.isEmpty()) continue
            if (entry.isDirectory) {
              SystemFileSystem.createDirectories(Path(installDir, realName))
            } else {
              progress?.emit(InstallFileEvent(realName))
              val path = Path(installDir, realName)
              SystemFileSystem.sink(path).use { sink -> source.readTo(sink, entry.realSize) }
              if (posix) setPosixPermissions(entry.mode, path)
            }
            progress?.emit(InstallProgressEvent(((size - buffer.size)).toFloat() / size.toFloat()))
          }
        }
      }
    }
    progress?.emit(InstallCompletedEvent)
  }

  private fun setPosixPermissions(mode: Int, path: Path) {
    val mode: Int = mode and 511
    if (mode > 0) {
      val perms = mutableSetOf<PosixFilePermission>()
      if (mode and 1 > 0) perms.add(PosixFilePermission.OTHERS_EXECUTE)
      if (mode and 2 > 0) perms.add(PosixFilePermission.OTHERS_WRITE)
      if (mode and 4 > 0) perms.add(PosixFilePermission.OTHERS_READ)
      if (mode and 8 > 0) perms.add(PosixFilePermission.GROUP_EXECUTE)
      if (mode and 16 > 0) perms.add(PosixFilePermission.GROUP_WRITE)
      if (mode and 32 > 0) perms.add(PosixFilePermission.GROUP_READ)
      if (mode and 64 > 0) perms.add(PosixFilePermission.OWNER_EXECUTE)
      if (mode and 128 > 0) perms.add(PosixFilePermission.OWNER_WRITE)
      if (mode and 256 > 0) perms.add(PosixFilePermission.OWNER_READ)
      Files.setPosixFilePermissions(Paths.get(path.toString()), perms)
    }
  }

  companion object {
    private const val BUFFER = 1024 * 1024L
    private const val PROGRESS_INTERVAL = 20

    const val CONFIG_FILE = "elide.json"
    const val STAMP_FILE = "stampfile"
    const val VERSION_FILE = "version"
    const val PROJECT_VERSION_FILE = ".elideversion"

    val PLATFORM = HostPlatform.resolve()
    val BINARY_NAME = if (PLATFORM.os == HostPlatform.OperatingSystem.WINDOWS) "elide.exe" else "elide"
    val GLOBAL_CONFIG_PATH =
      when (PLATFORM.os) {
        HostPlatform.OperatingSystem.LINUX -> "/etc/elide/$CONFIG_FILE"
        HostPlatform.OperatingSystem.DARWIN -> "/Library/Preferences/elide/$CONFIG_FILE"
        HostPlatform.OperatingSystem.WINDOWS -> "C:\\ProgramData\\elide\\$CONFIG_FILE"
      }
    val DEFAULT_INSTALL_PATH =
      when (PLATFORM.os) {
        HostPlatform.OperatingSystem.LINUX -> "/usr/local/share/elide"
        HostPlatform.OperatingSystem.DARWIN -> "/Applications/elide"
        HostPlatform.OperatingSystem.WINDOWS -> "C:\\Program Files\\elide"
      }
    val HOME: String = System.getProperty("user.home")
    val LINUX_CONFIG_HOME = System.getenv("XDG_CONFIG_HOME") ?: "$HOME/.config"
    val LINUX_INSTALL_HOME = System.getenv("XDG_DATA_HOME") ?: "$HOME/.local/share"
    val USER_CONFIG_PATH =
      when (PLATFORM.os) {
        HostPlatform.OperatingSystem.LINUX -> "$LINUX_CONFIG_HOME/elide/$CONFIG_FILE"
        HostPlatform.OperatingSystem.DARWIN -> "$HOME/Library/Application Support/config/elide/$CONFIG_FILE"
        HostPlatform.OperatingSystem.WINDOWS -> "$HOME\\AppData\\Local\\elide\\config\\$CONFIG_FILE"
      }
    // TODO: get default repositories when they exist
    val DEFAULT_REPOSITORIES = listOf<String>()
    val DEFAULT_CONFIG =
      ElideInstallConfig(
        when (PLATFORM.os) {
          HostPlatform.OperatingSystem.LINUX ->
            listOf("/usr/local/share/elide", "$LINUX_INSTALL_HOME/elide")

          HostPlatform.OperatingSystem.DARWIN -> listOf("/Applications/elide", "$HOME/Applications/elide")
          HostPlatform.OperatingSystem.WINDOWS ->
            listOf("C:\\Program Files\\elide", "$HOME\\AppData\\Local\\elide\\install")
        },
        if (PLATFORM.os == HostPlatform.OperatingSystem.LINUX) listOf("/usr/share/elide") else emptyList(),
        DEFAULT_REPOSITORIES,
        when (PLATFORM.os) {
          HostPlatform.OperatingSystem.LINUX -> "/usr/local/share/elide"
          HostPlatform.OperatingSystem.DARWIN -> "/Applications/elide"
          HostPlatform.OperatingSystem.WINDOWS -> "C:\\Program Files\\elide"
        },
      )
  }
}
