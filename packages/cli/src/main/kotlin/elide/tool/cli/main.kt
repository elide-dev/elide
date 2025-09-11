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

@file:Suppress("NOTHING_TO_INLINE")

package elide.tool.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parsers.CommandLineParser
import com.github.ajalt.clikt.parsers.flatten
import io.micronaut.configuration.picocli.MicronautFactory
import org.graalvm.nativeimage.ImageInfo
import org.slf4j.bridge.SLF4JBridgeHandler
import picocli.CommandLine
import java.nio.file.Files
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess
import elide.runtime.gvm.kotlin.KotlinLanguage
import elide.tool.cli.Elide.Companion.installStatics
import elide.tool.cli.cmd.repl.HandledExit
import elide.tooling.cli.Statics

// Whether to enable the experimental V2 entrypoint through Clikt.
private const val ENABLE_CLI_ENTRY_V2 = false

// Whether to log early init messages.
private const val EARLY_INIT_LOG = false

// Whether to exist after completion.
internal var exitOnComplete = true

// Exit code for this run.
@Volatile internal var exitCode = 0

// Unhandled error that caused exit, if any.
internal val unhandledExc = atomic<Throwable?>(null)

private inline fun earlyLog(msg: String) {
  if (EARLY_INIT_LOG) println("[init] $msg")
}

@Suppress("SpreadOperator")
private fun createApplicationContext(args: Array<String>) = applicationContextBuilder.args(*args)

// Read the old entrypoint factory.
private fun sorryIHaveToFactory(args: Array<String>): CommandLine =
  createApplicationContext(args)
  .start()
  .use {
    CommandLine(Elide::class.java, MicronautFactory(it))
  }

// Run the Clikt or regular entrypoint.
@Suppress("TooGenericExceptionCaught")
private inline fun runInner(args: Array<String>): Int = when (ENABLE_CLI_ENTRY_V2) {
  false -> Elide.entry(args)
  true -> createApplicationContext(args).start().use { applicationContext ->
      MicronautFactory(applicationContext).use { factory ->
        runCatching {
          val procInfo = ProcessHandle.current().info()
          val cmd = procInfo.command().orElse("elide")

          installStatics(
            cmd,
            args,
            System.getProperty("user.dir"),
          )
          CommandLineParser
            .parse(factory.create(Elide::class.java), args.toList())
        }.onFailure {
          println("Failed to parse arguments: ${it.message}")
          it.printStackTrace()
        }.getOrNull()?.let { command ->
          try {
            val cmd = command.invocation
              .flatten()
              .first()
              .command

            runBlocking(Dispatchers.Unconfined) {
              cmd.enter().exitCode
            }
          } catch (_: PrintHelpMessage) {
            try {
              sorryIHaveToFactory(args).usage(System.out)
            } catch (err: Throwable) {
              println("Failed to print help message: ${err.message}")
              err.printStackTrace()
              exitProcess(1)
            }
            0
          } catch (err: RuntimeException) {
            println("Uncaught error while running command: ${err.message}")
            err.printStackTrace()
            1
          }
        } ?: 1
      }
    }
}

internal object NativeEntry {
  @JvmName("enter") @JvmStatic
  fun enter(args: Array<String>): Int = entry(args, true)
}

// Perform early startup initialization tasks.
@Volatile var entryInitialized: Boolean = false

inline fun setStaticProperties(binPath: String) {
  // Patch the Java library path to include the binary's own parent directory.
  val currentJavaPath = System.getProperty("java.library.path")
  val path = Path(binPath).parent
  var newJavaPath = currentJavaPath
  newJavaPath = "$path:$newJavaPath"
  System.setProperty("java.library.path", newJavaPath)

  System.setProperty("elide.js.vm.enableStreams", "true")
  System.setProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true")
  System.setProperty("java.util.logging.config.class", "elide.tool.cli.InertLoggerConfigurator")
  System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Host,Content-Length")
  System.setProperty("jansi.eager", "false")
  System.setProperty("io.netty.allocator.maxOrder", "3")
  System.setProperty("io.netty.serviceThreadPrefix", "elide-svc")
  System.setProperty("io.netty.native.deleteLibAfterLoading", "true")
  System.setProperty("io.netty.buffer.bytebuf.checkAccessible", "false")
  System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "2")
  System.setProperty("kotlinx.coroutines.scheduler.core.pool.size", "2")
  System.setProperty("kotlinx.coroutines.scheduler.max.pool.size", "2")
  System.setProperty("kotlinx.coroutines.scheduler.default.name", "ElideDefault")

  System.setProperty(org.fusesource.jansi.AnsiConsole.JANSI_MODE, org.fusesource.jansi.AnsiConsole.JANSI_MODE_FORCE)
  System.setProperty(org.fusesource.jansi.AnsiConsole.JANSI_GRACEFUL, "false")

  // if no java home property is set, and an env variable is set, propagate it
  val embeddedJavaHome = path.resolve("resources").resolve("gvm").also { jvmPath ->
    System.setProperty("elide.java.home", jvmPath.absolutePathString())
  }
  if (System.getProperty("java.home") == null) {
    when (System.getenv("JAVA_HOME")?.ifBlank { null }?.let { javaHome ->
      System.setProperty("java.home", javaHome)
    }) {
      null -> {
        // if no java home is set at all, and elide shipped with an embedded jvm, use that
        System.setProperty("java.home", embeddedJavaHome.absolutePathString())
      }

      // otherwise do nothing
      else -> {}
    }
  }

  // only set the stdlib/reflect paths if we are in a native image context. otherwise, the "binary path" may be a path
  // to the java binary, rather than elide.
  if (ImageInfo.inImageCode()) {
    // kotlin path; only used if kotlinc is invoked
    val kotlinLibsPath = path
      .resolve("resources")
      .resolve("kotlin")
      .resolve(KotlinLanguage.VERSION)
      .resolve("lib")

    // kotlin stdlib and reflect paths
    val kotlinStdlibPath = kotlinLibsPath.resolve("kotlin-stdlib.jar")
    val kotlinReflectPath = kotlinLibsPath.resolve("kotlin-reflect.jar")

    System.setProperty("kotlin.java.stdlib.jar", kotlinStdlibPath.absolutePathString())
    System.setProperty("kotlin.java.reflect.jar", kotlinReflectPath.absolutePathString())
  }
}

private fun resolveBinaryPathForNative(): String {
  // Prefer a fully-qualified, real path without relying on the process working dir.
  val cmd = ProcessHandle.current().info().command().orElse(null)
  if (cmd != null) {
    val p = Path(cmd)
    // If already absolute, resolve any symlinks and return.
    if (p.isAbsolute) {
      return try {
        (if (Files.isSymbolicLink(p)) p.toRealPath() else p).absolutePathString()
      } catch (_: Throwable) {
        p.toAbsolutePath().absolutePathString()
      }
    }
  }

  // If the command was relative (common on Linux/WSL), fall back to /proc/self/exe which is
  // always absolute and does not depend on user.dir.
  return try {
    Path("/proc/self/exe").toRealPath().absolutePathString()
  } catch (_: Throwable) {
    // Last resort: return the raw command as-is (may be relative)
    cmd ?: "elide"
  }
}

private fun safeWorkingDirectory(): String {
  // Try the standard property first; if it fails or is blank, fall back to Linux /proc,
  // then environment variables (PWD/HOME), and finally "." as a last resort.
  val primary = try { System.getProperty("user.dir") } catch (_: Throwable) { null }
    ?.takeIf { it.isNotBlank() }
  if (primary != null) return primary
  return try {
    Path("/proc/self/cwd").toRealPath().absolutePathString()
  } catch (_: Throwable) {
    System.getenv("PWD")?.takeIf { it.isNotBlank() }
      ?: System.getenv("HOME")?.takeIf { it.isNotBlank() }
      ?: "."
  }
}

// Visible for tests in the same module.
internal fun safeWorkingDirectoryForTest(): String = safeWorkingDirectory()

fun initializeEntry(args: Array<String>, installStatics: Boolean = true) {
  if (entryInitialized) return
  entryInitialized = true

  earlyLog("Setting static properties")
  val binPath = when (ImageInfo.inImageRuntimeCode()) {
    true -> resolveBinaryPathForNative()
    false -> requireNotNull(System.getProperty("elide.gvmResources")) {
      "Failed to resolve `elide.resources` property"
    }
  }
  setStaticProperties(binPath)
  if (installStatics) {
    earlyLog("Installing statics")
    Statics.mountArgs(binPath, args)
    installStatics(binPath, args, safeWorkingDirectory())
    earlyLog("Installing bridge handler for SLF4j")
    SLF4JBridgeHandler.install()
  }
}

/**
 * Main entrypoint for Elide on the command line.
 *
 * This entrypoint method will call [exitProcess] with the exit code of the program; for testing or API-based dispatch,
 * see other entrypoints.
 *
 * @param args Arguments to run with.
 */
@Suppress("TooGenericExceptionCaught")
fun entry(args: Array<String>, installStatics: Boolean): Int = runBlocking(Dispatchers.Unconfined) {
  try {
    initializeEntry(args, installStatics)
    runInner(args)
  } catch (err: RuntimeException) {
    when {
      HandledExit.isHandledExit(err) -> (err as HandledExit).exitCode

      else -> {
        unhandledExc.compareAndSet(null, err)
        throw err
      }
    }
  }
}

/**
 * Main entrypoint for Elide on the command line.
 *
 * This entrypoint method will call [exitProcess] with the exit code of the program; for testing or API-based dispatch,
 * see other entrypoints.
 *
 * @param args Arguments to run with.
 */
@Suppress("TooGenericExceptionCaught")
fun main(args: Array<String>): Unit = try {
  // perform early init
  earlyLog("Initializing entrypoint")
  initializeEntry(args)

  // run the entrypoint
  exitCode = try {
    earlyLog("Passing to inner entrypoint")
    runInner(args)
  } catch (err: RuntimeException) {
    when (err) {
      is HandledExit -> err.exitCode
      else -> {
        unhandledExc.compareAndSet(null, err)
        1
      }
    }
  }
} finally {
  Elide.close()
}.also {
  // exit the process if the global exit flag is set
  if (exitOnComplete) exitProcess(exitCode)
}
