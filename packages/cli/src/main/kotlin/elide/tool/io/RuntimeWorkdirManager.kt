/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.tool.io

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import jakarta.inject.Provider
import kotlin.io.path.absolute
import kotlin.io.path.exists
import elide.annotations.Context
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.HostPlatform
import elide.runtime.core.HostPlatform.OperatingSystem.*
import elide.tool.cli.Elide
import elide.tool.io.WorkdirManager.WorkdirHandle

/** Main implementation of the runtime working directory manager. */
internal class RuntimeWorkdirManager : WorkdirManager {
  internal object SingletonManager {
    @JvmStatic val singleton: AtomicReference<RuntimeWorkdirManager> = AtomicReference(null)
  }

  internal companion object {
    private const val nativesDir = "native"
    private const val cachesDir = "caches"
    private const val flightRecorderDir = "blackbox"
    private const val nixTempPath = "/tmp/elide-runtime"
    private const val runtimeDirPrefix = "elide-runtime-"
    private const val elideHomeDirectory = ".elide"
    private const val elideConfigDirectory = "elide"
    private const val configDirectory = ".config"

    private val linuxCachesPath = "~/elide/caches/v${Elide.version()}"
    private val darwinCachesPath = "/Library/caches/elide/v${Elide.version()}"
    private val projectAnchorFiles = arrayOf(
      ".git",
      "package.json",
      "pkg.elide.yml",
      "pkg.elide.yaml",
      "pkg.elide.toml",
      "pkg.elide.json",
      "pkg.elide.js",
      "pkg.elide.kts",
      "pkg.elide.py",
      "pkg.elide.rb",
    )

    private fun persistentTempPath(): Path = when (HostPlatform.resolve().os) {
      DARWIN, LINUX -> File("$nixTempPath/v${Elide.version()}")
        .toPath()
        .absolute()

      WINDOWS -> File((System.getenv("localappdata") ?: error("No local app data folder")))
        .resolve("Temp")
        .resolve("Elide")
        .resolve("v${Elide.version()}")
        .toPath()
        .absolute()
    }

    /** @return Created or acquired [RuntimeWorkdirManager] singleton. */
    @JvmStatic fun acquire(): RuntimeWorkdirManager = synchronized(this) {
      if (SingletonManager.singleton.get() == null) {
        SingletonManager.singleton.set(RuntimeWorkdirManager())
      }
      SingletonManager.singleton.get()
    }
  }

  /** Provides an injection factory for resolving the singleton [RuntimeWorkdirManager]. */
  @Factory class DefaultRuntimeWorkdirManagerProvider : Provider<RuntimeWorkdirManager> {
    @Context @Singleton override fun get(): RuntimeWorkdirManager = acquire()
  }

  @Volatile private var active: Boolean = true
  @Volatile private var initialized: Boolean = false
  @Volatile private lateinit var rootDirectory: Path
  @Volatile private lateinit var tempDirectory: Path
  private val handleCache: SortedMap<File, WorkdirHandle> = ConcurrentSkipListMap()

  private fun currentSharedTempPrefix(): String {
    return StringBuilder().apply {
      append(runtimeDirPrefix)
      append(Elide.version())
    }.toString()
  }

  private fun obtainHandle(file: File, read: Boolean, write: Boolean): WorkdirHandle = handleCache.getOrPut(file) {
    val knownExists = AtomicBoolean(false)
    val cachedReadable = AtomicBoolean(false)
    val cachedWritable = AtomicBoolean(false)

    fun guarantees(producer: () -> File): File {
      val target = producer()
      if (!knownExists.get() && !target.exists()) {
        if (read || write) {
          Files.createDirectories(target.toPath().toAbsolutePath())
          knownExists.compareAndSet(false, true)
        }
      }
      return target
    }

    fun fileExists(): Boolean {
      val exists = file.exists()
      if (exists) {
        knownExists.set(true)
      }
      return exists
    }

    fun isWritable(): Boolean {
      val writable = file.canWrite()
      if (writable) {
        cachedWritable.set(true)
      }
      return writable
    }

    fun isReadable(): Boolean {
      val readable = file.canRead()
      if (readable) {
        cachedReadable.set(true)
      }
      return readable
    }

    object : WorkdirHandle {
      override val exists: Boolean get() = knownExists.get() || fileExists()

      override val writable: Boolean get() = toFile().let {
        cachedWritable.get() || isWritable()
      }

      override val readable: Boolean get() = toFile().let {
        cachedReadable.get() || isReadable()
      }

      override fun toFile(): File = guarantees {
        file
      }

      override fun toPath(): Path = toFile().toPath()
    }
  }

  private fun File.toHandle(read: Boolean = true, write: Boolean = true): WorkdirHandle = obtainHandle(
    this,
    read,
    write,
  )

  private fun initializeTemporaryWorkdir(): Path = synchronized(this) {
    if (initialized) return tempDirectory
    initialized = true
    Files.createDirectories(persistentTempPath()).also {
      rootDirectory = it
    }
  }

  private fun obtainWorkdir(): Path =
    if (initialized) rootDirectory else initializeTemporaryWorkdir()

  private fun workSubdir(
    name: String,
    temporary: Boolean = true,
    lazy: Boolean = false,
  ): Path = obtainWorkdir().resolve(name).apply {
    prepareDirectory(this, temporary, lazy)
  }

  // Find the nearest parent directory to `cwd` with one of the provided `files` present.
  private fun nearestDirectoryWithAnyOfTheseFiles(
    files: Array<String>,
    base: File? = null,
    depth: Int? = null,
  ): File? {
    val currentDepth = depth ?: 0
    if ((base != null && (base.parentFile == null || base.absolutePath == "/")) || files.isEmpty())
      return null  // can't search base
    if (currentDepth > 15) return null  // don't search too deep
    val cwd = (base ?: File(System.getProperty("user.dir"))).apply {
      // if the cwd doesn't exist, we're jailed and can't find it anyway
      if (base == null && !exists()) return null
      else if (base != null && base.parentFile == null) return null
    }

    val cwdFiles = cwd.listFiles()
    if (cwdFiles != null) {
      val cwdFileNames = cwdFiles.map { it.name }.toSortedSet()
      val found = files.intersect(cwdFileNames)
      if (found.isNotEmpty()) {
        return cwd
      }
    }
    return nearestDirectoryWithAnyOfTheseFiles(files, cwd.parentFile, currentDepth + 1)
  }

  // Find the nearest parent directory with one of the provided `projectAnchorFiles`.
  private fun walkLocateProjectRoot(base: File? = null): File? = nearestDirectoryWithAnyOfTheseFiles(
    projectAnchorFiles,
    base,
  )

  private fun prepareDirectory(
    target: Path,
    temporary: Boolean = true,
    lazy: Boolean = false,
    exists: Boolean = false,
  ): Path = target.apply {
    if (temporary) {
      toFile().deleteOnExit()
    }
    if (!lazy && !exists && !target.exists()) {
      Files.createDirectories(this)
    }
  }

  // Require the workdir manager to be active for the provided operation.
  private inline fun <R> requireActive(op: () -> R): R {
    assert(active) { "Cannot use runtime temporary directory resources after the manager has closed" }
    return op.invoke()
  }

  // Handle the `~` symbol in path references.
  @OptIn(DelicateElideApi::class)
  private fun userDir(path: String): Path {
    if (path.startsWith("~")) {
      return Path.of(path.replace("~", when (HostPlatform.resolve().os) {
        DARWIN, LINUX -> "/home/${System.getProperty("user.name")}"
        else -> error("Windows paths should not use `~`")
      }))
    }
    return Path.of(path)
  }

  // Native library directory.
  private val nativesDirectory by lazy {
    workSubdir(nativesDir, temporary = false, lazy = false)
  }

  // Temporary (delete-on-exit) directory.
  private val temporaryDirectory by lazy {
    obtainWorkdir()
    tempDirectory = Files.createTempDirectory(currentSharedTempPrefix())
    prepareDirectory(tempDirectory, temporary = true, lazy = true)
  }

  // Persistent flight recorder/error recording directory.
  private val flightRecorderDirectory by lazy {
    workSubdir(flightRecorderDir, temporary = false, lazy = true)
  }

  // User-level configurations for Elide.
  private val userConfigDirectory by lazy {
    val homeDir = Paths.get(System.getProperty("user.home"))
    val defaultPath = homeDir.resolve(configDirectory).resolve(elideConfigDirectory)

    val userConfigPaths = listOf(
      homeDir.resolve(elideHomeDirectory),
      defaultPath,
    )

    (userConfigPaths.firstOrNull {
      // the first one that exists, wins, and if nothing exists, the default path is used
      it.toAbsolutePath().exists()
    } ?: defaultPath).toFile()
  }

  // Root directory for the current project, as applicable.
  private val projectRootDirectory by lazy {
    walkLocateProjectRoot()
  }

  @OptIn(DelicateElideApi::class)
  private val cachesDirectory by lazy {
    when (HostPlatform.resolve().os) {
      DARWIN -> prepareDirectory(userDir(darwinCachesPath), temporary = false, lazy = true)
      LINUX -> prepareDirectory(userDir(linuxCachesPath), temporary = false, lazy = true)
      else -> workSubdir(cachesDir, temporary = false, lazy = true)
    }
  }

  override fun configRoot(): WorkdirHandle = requireActive {
    userConfigDirectory.toHandle()
  }

  override fun projectRoot(): WorkdirHandle? = requireActive {
    projectRootDirectory?.toHandle()
  }

  override fun workingRoot(): File = requireActive {
    obtainWorkdir().toFile()
  }

  override fun nativesDirectory(): WorkdirHandle = requireActive {
    nativesDirectory.toFile().toHandle()
  }

  override fun tmpDirectory(create: Boolean): WorkdirHandle = requireActive {
    temporaryDirectory.toFile().toHandle()
  }

  override fun flightRecorderDirectory(create: Boolean): WorkdirHandle = requireActive {
    flightRecorderDirectory.toFile().toHandle()
  }

  override fun cacheDirectory(create: Boolean): WorkdirHandle = requireActive {
    cachesDirectory.toFile().toHandle()
  }

  override fun close() {
    active = false
  }
}
