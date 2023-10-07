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

package elide.tool.io

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.SortedMap
import java.util.SortedSet
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
import elide.tool.cli.ElideTool
import elide.tool.io.WorkdirManager.WorkdirHandle

/** Main implementation of the runtime working directory manager. */
internal class RuntimeWorkdirManager : WorkdirManager {
  internal companion object {
    private const val nativesDir = "native"
    private const val tempDir = "temp"
    private const val cachesDir = "caches"
    private const val flightRecorderDir = "blackbox"
    private const val nixTempPath = "/tmp/elide-runtime"
    private const val runtimeDirPrefix = "elide-runtime-"
    private const val elideHomeDirectory = ".elide"
    private const val elideConfigDirectory = "elide"
    private const val configDirectory = ".config"

    private val linuxCachesPath = "~/elide/caches/v${ElideTool.version()}"
    private val darwinCachesPath = "/Library/caches/elide/v${ElideTool.version()}"
    private val projectAnchorFiles = sortedSetOf(
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

    @OptIn(DelicateElideApi::class)
    private val persistentTempPath = when (HostPlatform.resolve().os) {
      DARWIN, LINUX -> File("$nixTempPath/v${ElideTool.version()}")
        .toPath()
        .absolute()

      WINDOWS -> File((System.getenv("localappdata") ?: error("No local app data folder")))
        .resolve("Temp")
        .resolve("Elide")
        .resolve("v${ElideTool.version()}")
        .toPath()
        .absolute()
    }

    private val singleton: AtomicReference<RuntimeWorkdirManager> = AtomicReference(null)

    /** @return Created or acquired [RuntimeWorkdirManager] singleton. */
    @JvmStatic fun acquire(): RuntimeWorkdirManager = synchronized(this) {
      if (singleton.get() == null) {
        singleton.set(RuntimeWorkdirManager())
      }
      singleton.get()
    }
  }

  /** Provides an injection factory for resolving the singleton [RuntimeWorkdirManager]. */
  @Factory class DefaultRuntimeWorkdirManagerProvider : Provider<RuntimeWorkdirManager> {
    @Context @Singleton override fun get(): RuntimeWorkdirManager = acquire()
  }

  private val active: AtomicBoolean = AtomicBoolean(true)
  private val initialized: AtomicBoolean = AtomicBoolean(false)
  private val rootDirectory: AtomicReference<File> = AtomicReference(null)
  private val tempDirectory: AtomicReference<File> = AtomicReference(null)
  private val handleCache: SortedMap<File, WorkdirHandle> = ConcurrentSkipListMap()

  private fun currentSharedTempPrefix(): String {
    return StringBuilder().apply {
      append(runtimeDirPrefix)
      append(ElideTool.version())
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

  private fun initializeTemporaryWorkdir(): File = synchronized(this) {
    if (initialized.get()) return tempDirectory.get()
    initialized.compareAndSet(false, true)
    tempDirectory.compareAndSet(null, Files.createTempDirectory(currentSharedTempPrefix()).toFile())
    rootDirectory.compareAndSet(null, Files.createDirectories(persistentTempPath).toFile())
    rootDirectory.get()
  }

  private fun obtainWorkdir(): File = if (initialized.get()) {
    rootDirectory.get()
  } else initializeTemporaryWorkdir()

  private fun workSubdir(
    name: String,
    temporary: Boolean = true,
    lazy: Boolean = false,
  ): File = obtainWorkdir().resolve(name).apply {
    prepareDirectory(this, temporary, lazy)
  }

  // Find the nearest parent directory to `cwd` with one of the provided `files` present.
  private fun nearestDirectoryWithAnyOfTheseFiles(files: SortedSet<String>, base: File? = null): File? {
    if (base?.path == "/")
      return null  // can't search base
    val cwd = (base ?: File(System.getProperty("user.dir"))).apply {
      // if the cwd doesn't exist, we're jailed and can't find it anyway
      if (base == null && !exists()) return null
    }

    val cwdFiles = cwd.listFiles()
    if (cwdFiles != null) {
      val cwdFileNames = cwdFiles.map { it.name }.toSortedSet()
      val found = files.intersect(cwdFileNames)
      if (found.isNotEmpty()) {
        return cwd
      }
    }
    return nearestDirectoryWithAnyOfTheseFiles(files, cwd.parentFile)
  }

  // Find the nearest parent directory with one of the provided `projectAnchorFiles`.
  private fun walkLocateProjectRoot(base: File? = null): File? = nearestDirectoryWithAnyOfTheseFiles(
    projectAnchorFiles,
    base,
  )

  private fun prepareDirectory(
    target: File,
    temporary: Boolean = true,
    lazy: Boolean = false,
    exists: Boolean = false,
  ): File = target.apply {
    if (temporary) {
      deleteOnExit()
    }
    if (!lazy && !exists && !target.exists()) {
      Files.createDirectories(toPath())
    }
  }

  // Require the workdir manager to be active for the provided operation.
  private inline fun <R> requireActive(op: () -> R): R {
    require(active.get()) {
      "Cannot use runtime temporary directory resources after the manager has closed"
    }
    return op.invoke()
  }

  // Handle the `~` symbol in path references.
  @OptIn(DelicateElideApi::class)
  private fun userDir(path: String): String {
    if (path.startsWith("~")) {
      return path.replace("~", when (HostPlatform.resolve().os) {
        DARWIN, LINUX -> "/home/${System.getProperty("user.name")}"
        else -> error("Windows paths should not use `~`")
      })
    }
    return path
  }

  // Native libraries directory.
  private val nativesDirectory by lazy {
    workSubdir(nativesDir, temporary = false, lazy = false)
  }

  // Temporary (delete-on-exit) directory.
  private val temporaryDirectory by lazy {
    obtainWorkdir()
    prepareDirectory(tempDirectory.get(), temporary = true, lazy = true)
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

    (userConfigPaths.first {
      // the first one that exists, wins, and if nothing exists, the default path is used
      it.toAbsolutePath().exists()
    } ?: defaultPath).let {
      it.toFile()
    }
  }

  // Root directory for the current project, as applicable.
  private val projectRootDirectory by lazy {
    walkLocateProjectRoot()
  }

  @OptIn(DelicateElideApi::class)
  private val cachesDirectory by lazy {
    when (HostPlatform.resolve().os) {
      DARWIN -> prepareDirectory(File(userDir(darwinCachesPath)), temporary = false, lazy = true)
      LINUX -> prepareDirectory(File(userDir(linuxCachesPath)), temporary = false, lazy = true)
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
    obtainWorkdir()
  }

  override fun nativesDirectory(): WorkdirHandle = requireActive {
    nativesDirectory.toHandle()
  }

  override fun tmpDirectory(create: Boolean): WorkdirHandle = requireActive {
    temporaryDirectory.toHandle()
  }

  override fun flightRecorderDirectory(create: Boolean): WorkdirHandle = requireActive {
    flightRecorderDirectory.toHandle()
  }

  override fun cacheDirectory(create: Boolean): WorkdirHandle = requireActive {
    cachesDirectory.toHandle()
  }

  override fun close() {
    active.compareAndSet(false, true)
  }
}
