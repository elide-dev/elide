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

@file:Suppress(
  "TooManyFunctions",
  "WildcardImport",
)

package elide.tool.engine

import io.netty.util.internal.PlatformDependent
import org.graalvm.nativeimage.ImageInfo
import org.sqlite.SQLiteJDBCLoader
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.Path
import elide.runtime.LogLevel.DEBUG
import elide.runtime.core.HostPlatform
import elide.runtime.core.HostPlatform.OperatingSystem.*
import elide.tooling.cli.Statics
import elide.tool.io.WorkdirManager

/**
 * # Native Engine
 *
 * Provides utilities for loading native portions of Elide early in the boot lifecycle.
 */
object NativeEngine {
  private const val DEFAULT_NATIVES_PATH = "META-INF/native/"
  private val transportEngine: AtomicReference<String> = AtomicReference("nio")
  private val nativeTransportAvailable: AtomicBoolean = AtomicBoolean(false)
  private val errHolder: AtomicReference<Throwable?> = AtomicReference(null)
  private val missingLibraries: MutableSet<NativeLibInfo> = ConcurrentSkipListSet()
  private val nativeLibraryGroups: MutableMap<String, Boolean> = ConcurrentSkipListMap()
  private val staticJniMode: Boolean = System.getProperty("elide.staticJni") == "true" && ImageInfo.inImageCode()

  internal fun transportEngine(): Pair<String, Boolean> = transportEngine.get() to nativeTransportAvailable.get()

  @JvmStatic private fun libExtension(os: HostPlatform.OperatingSystem): String? = when (os) {
    DARWIN -> "jnilib"
    LINUX -> "so"
    WINDOWS -> "dll"
  }

  // Calculate the path to a native resource which needs to be loaded.
  @JvmStatic private fun nativeLib(
    group: String,
    staticName: String,
    os: HostPlatform.OperatingSystem,
    base: String = DEFAULT_NATIVES_PATH,
    extension: String? = libExtension(os),
  ): NativeLib {
    val libname = StringBuilder().apply {
      // `umbrella` ...
      append(staticName)
    }

    val path = StringBuilder(base).apply {
      // `META-INF/native/libumbrella` ...
      append("lib")
      append(libname)

      // `META-INF/native/libumbrella.so` ...
      if (extension?.isNotBlank() == true) {
        append(".")
        append(extension)
      }
    }.toString()

    return NativeLib.of(
      group,
      libname.toString(),
      path,
      staticName,
    )
  }

  // Calculate the path to a Netty native resource which needs to be loaded.
  @JvmStatic private fun nettyNativeLib(
    group: String,
    staticName: String,
    base: String = DEFAULT_NATIVES_PATH,
    os: HostPlatform.OperatingSystem,
    extension: String? = libExtension(os),
    builder: StringBuilder.() -> Unit,
  ): NativeLib {
    val libname = StringBuilder().apply {
      // `netty_transport_native_epoll_linux-x86_64` ...
      builder.invoke(this)
    }

    val path = StringBuilder(base).apply {
      // `META-INF/native/libnetty_transport_native_epoll_linux-x86_64` ...
      append("lib")
      append(libname)

      // `META-INF/native/libnetty_transport_native_epoll_linux-x86_64.so` ...
      if (extension?.isNotBlank() == true) {
        append(".")
        append(extension)
      }
    }.toString()

    return NativeLib.of(
      group,
      libname.toString(),
      path,
      staticName,
    )
  }

  // Calculate the proper name of a Netty native library that needs to be loaded, based on the host OS.
  @JvmStatic private fun nettyNative(
    group: String,
    name: String,
    os: HostPlatform.OperatingSystem,
    static: String = name,
  ): NativeLib {
    val arch = PlatformDependent.normalizedArch()
    return nettyNativeLib(group = group, os = os, staticName = static) {
      // `netty_` ...
      append("netty_")

      // `netty_transport_native_epoll` ...
      append(name)

      if (!staticJniMode) {
        // `netty_transport_native_epoll_linux-x86_64` ...
        append("_")
        append(arch)
      }
    }
  }

  // Ensure a native library is loadable from the Java native path, copying from resources if necessary.
  @Suppress("LongParameterList")
  @JvmStatic private fun ensureLoadableFromNatives(
    group: String,
    libs: List<NativeLibInfo>,
    workdirProvider: () -> File,
    loader: ClassLoader,
    allCandidatePaths: Sequence<Path>,
    forceLoad: Boolean = false,
  ) = libs.map {
    val (loaded, copied) = try {
      NativeUtil.loadOrCopy(
        workdirProvider,
        it.path,
        it.name,
        loader,
        allCandidatePaths,
        forceLoad = forceLoad,
      )
    } catch (_: Throwable) {
      missingLibraries.add(it)
      false to false
    }

    if (Statics.logging.isEnabled(DEBUG)) {
      Statics.logging.debug("Native library ${it.name} (loaded=$loaded, copied=$copied)")
    }
    loaded
  }.all { it }.also {
    nativeLibraryGroups[group] = it
  }

  // Load a named native library, varying by OS.
  @Suppress("LongParameterList")
  @JvmStatic private fun HostPlatform.loadByPlatform(
    loader: ClassLoader,
    group: String,
    workdirProvider: () -> File,
    allCandidatePaths: Sequence<Path>,
    forceLoad: Boolean = !ImageInfo.inImageCode(),
    linux: (() -> List<NativeLibInfo>?)? = null,
    darwin: (() -> List<NativeLibInfo>?)? = null,
    windows: (() -> List<NativeLibInfo>?)? = null,
    default: (() -> List<NativeLibInfo>?)? = null,
  ) {
    when (os) {
      LINUX -> linux
      DARWIN -> darwin
      WINDOWS -> windows
    }.let {
      when (val target = (it?.invoke() ?: default?.invoke())) {
        null -> {}
        else -> ensureLoadableFromNatives(
          group,
          target,
          workdirProvider,
          loader,
          allCandidatePaths,
          forceLoad,
        ).also { result ->
          if (Statics.logging.isEnabled(DEBUG)) {
            Statics.logging.debug("Native library group $group (loaded=$result)")
          }
        }
      }
    }
  }

  // Load native tooling libraries, based on OS.
  @JvmStatic private fun HostPlatform.loadNativeTooling(
    workdirProvider: () -> File,
    allCandidatePaths: Sequence<Path>,
    loader: ClassLoader,
  ) = loadByPlatform(
    loader,
    "tools",
    workdirProvider,
    allCandidatePaths,
    default = {
      listOf(nativeLib("tools", "umbrella", os))
    },
  )

  // Load native transport libraries, based on OS.
  @JvmStatic private fun HostPlatform.loadNativeTransport(
    workdirProvider: () -> File,
    allCandidatePaths: Sequence<Path>,
    loader: ClassLoader,
  ) = loadByPlatform(
    loader,
    "transport",
    workdirProvider,
    allCandidatePaths,
    linux = { listOf(
      nettyNative("transport", "transport_native_epoll", os),
      // nettyNative("transport", "quiche_linux", os),
    ) },
    darwin = { listOf(
      // nettyNative("transport", "resolver_dns_native_macos", os),
      // nettyNative("transport", "quiche_osx", os),
      nettyNative("transport", "transport_native_kqueue", os),
    ) },
  )

  // Load native OpenSSL libraries.
  @JvmStatic private fun HostPlatform.loadNativeCrypto(
    workdirProvider: () -> File,
    allCandidatePaths: Sequence<Path>,
    loader: ClassLoader,
  ) = loadByPlatform(
    loader,
    "crypto",
    workdirProvider,
    allCandidatePaths,
    linux = {
      listOf(nettyNative("crypto", "tcnative_linux", os))
    },
    darwin = {
      listOf(nettyNative("crypto", "tcnative_osx", os))
    },
  )

  // Load natives from the CWD while it is swapped in.
  @Suppress("TooGenericExceptionCaught")
  @JvmStatic private fun loadApplicableNatives(
    platform: HostPlatform,
    nativesProvider: () -> File,
    server: Boolean,
    tooling: Boolean,
    allCandidatePaths: Sequence<Path>,
    loader: ClassLoader,
  ) = platform.apply {
    // in static JNI mode, we don't do any of this anymore
    val loadNatives: () -> Unit = if (ImageInfo.inImageCode() && staticJniMode) {
      // nothing to load
      {
        if (server) {
          loadNativeTransport(nativesProvider, allCandidatePaths, loader)
        }
      }
    } else {
      // trigger load of native libs
      {
        if (server) {
          loadNativeTransport(nativesProvider, allCandidatePaths, loader)
          loadNativeCrypto(nativesProvider, allCandidatePaths, loader)
        }
        if (tooling) {
          loadNativeTooling(nativesProvider, allCandidatePaths, loader)
        }
      }
    }

    try {
      when (platform.os) {
        // on linux, we prefer `epoll` unless `io_uring` is requested
        LINUX -> {{
          // load libraries
          loadNatives()

          // check and return availability of epoll
          when {
            io.netty.incubator.channel.uring.IOUring.isAvailable() ->
              io.netty.incubator.channel.uring.IOUring.ensureAvailability().let { "io_uring" to true }

            io.netty.channel.epoll.Epoll.isAvailable() ->
              io.netty.channel.epoll.Epoll.ensureAvailability().let { "epoll" to true }

            else -> "nio" to false
          }
        }}

        // on darwin, we prefer `kqueue`
        DARWIN -> {{
          // then force-load libraries
          loadNatives()

          // then check and load KQueue and return
          io.netty.channel.kqueue.KQueue.ensureAvailability().let { "kqueue" to true }
        }}

        // no native transport available
        else -> {{ "nio" to false }}
      }.invoke()
    } catch (err: Throwable) {
      errHolder.set(err)
      "nio" to false
    }.also { (label, available) ->
      transportEngine.set(label)
      nativeTransportAvailable.set(available)
    }
  }

  /**
   * ## Load Natives
   *
   * Prepare VM-level settings and trigger loading of critical native libraries.
   *
   * @param workdirProvider Provider which yields a working directory manager.
   * @param server Whether to initialize server components.
   * @param tooling Whether to initialize tooling components.
   * @param extraProps Extra VM properties to set.
   */
  @Suppress("UNUSED_PARAMETER")
  @JvmStatic fun load(
    workdirProvider: () -> WorkdirManager,
    server: Boolean,
    tooling: Boolean,
    extraProps: List<Pair<String, String>>,
  ) {

    val platform = HostPlatform.resolve()
    val separator = File.separator
    val libraryPath = System.getProperty("java.library.path", "")

    // sanity check: exec path should exist, then convert it into a suite of lib exec paths
    val resolvedExecPrefix = (if (ImageInfo.inImageCode()) {
      ProcessHandle.current().info().command().orElse(null)
    } else {
      // if we are on jvm mode, take the current directory
      System.getProperty("user.dir")
    } ?: error(
      "Failed to resolve exec prefix"
    )).let {
      Path.of(it)
    }
    val libExecPaths = listOf<Path>(
      resolvedExecPrefix,
      resolvedExecPrefix.resolve("python/python-home/lib/graalpy25.0"),
      resolvedExecPrefix.resolve("llvm/libsulong-native"),
      resolvedExecPrefix.resolve("ruby/ruby-home"),
    )
    val javaLibraryPath = System.getenv("JAVA_HOME")?.ifBlank { null }?.let {
      Path(it).resolve("lib")
    }
    val combinedFullPath = sequence<Path> {
      val natives = workdirProvider.invoke().nativesDirectory()
      yield(natives.toPath())
      yieldAll(libraryPath.split(separator).map { Path.of(it) })
      yieldAll(libExecPaths)
      javaLibraryPath?.let { yield(it) }
    }

    // load all required native libs for current platform
    loadApplicableNatives(
      platform,
      { workdirProvider.invoke().nativesDirectory().toFile() },
      server = server,
      tooling = tooling,
      combinedFullPath,
      this::class.java.classLoader,
    )

    // in jvm mode, force-load sqlite unless explicitly disabled
    if (!ImageInfo.inImageCode()) {
      val disableSqlite = (System.getProperty("elide.disable.sqlite") == "true") ||
        (System.getProperty("elide.disableNatives") == "true") ||
        (System.getenv("ELIDE_DISABLE_SQLITE")?.lowercase() == "true") ||
        (System.getenv("ELIDE_DISABLE_NATIVES")?.lowercase() == "true")
      if (!disableSqlite) {
        nativeLibraryGroups["sqlite"] = SQLiteJDBCLoader.initialize()
      }
    }

    // fix: account for static jni
    if (ImageInfo.inImageCode()) listOf(
      "console",
      "crypto",
      "sqlite",
      "transport",
      "tools",
    ).forEach {
      nativeLibraryGroups[it] = true
    }
  }

  /**
   * ## Load Check
   *
   * Checks to make sure a [group] of named native libraries was properly loaded.
   *
   * @param group Name of the group to check.
   * @return Whether the group was loaded.
   */
  @JvmStatic fun didLoad(group: String): Boolean =
    nativeLibraryGroups[group] ?: false

  /**
   * ## Boot Entrypoint
   *
   * Early in the static init process for Elide, this method is called to prepare and load native libraries and apply
   * VM-level settings as early as possible.
   *
   * @param workdirProvider Provider which yields a working directory manager.
   * @param server Whether to initialize server components.
   * @param tooling Whether to initialize tooling components.
   * @param properties Provider of extra VM properties to set.
   */
  @JvmStatic inline fun boot(
    noinline workdirProvider: () -> WorkdirManager,
    server: Boolean = true,
    tooling: Boolean = true,
    properties: () -> List<Pair<String, String>>
  ) {
    load(
      workdirProvider,
      server = server,
      tooling = tooling,
      properties.invoke(),
    )
  }
}
