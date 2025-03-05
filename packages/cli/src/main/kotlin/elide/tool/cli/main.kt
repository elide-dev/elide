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
import io.micronaut.context.ApplicationContext
import org.slf4j.bridge.SLF4JBridgeHandler
import picocli.CommandLine
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import elide.annotations.Eager
import elide.tool.cli.Elide.Companion.installStatics

// Whether to enable the experimental V2 entrypoint through Clikt.
private val ENABLE_CLI_ENTRY_V2 = System.getenv("ELIDE_EXPERIMENTAL")?.ifBlank { null } != null

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
private fun createApplicationContext(args: Array<String>) = ApplicationContext
  .builder()
  .eagerInitAnnotated(Eager::class.java)
  .args(*args)

// Read the old entrypoint factory.
private fun sorryIHaveToFactory(args: Array<String>): CommandLine =
  createApplicationContext(args)
  .start()
  .use { CommandLine(Elide::class.java, MicronautFactory(it)) }

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

inline fun setStaticProperties() {
  System.setProperty("elide.js.vm.enableStreams", "true")
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
}

fun initializeEntry(args: Array<String>, installStatics: Boolean = true) {
  if (entryInitialized) return
  entryInitialized = true

  earlyLog("Setting static properties")
  setStaticProperties()
  if (installStatics) {
    earlyLog("Installing statics")
    Statics.mountArgs(args)
    val binPath = ProcessHandle.current().info().command().orElse(null)
    installStatics(binPath, args, System.getProperty("user.dir"))
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
    unhandledExc.compareAndSet(null, err)
    throw err
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
    unhandledExc.compareAndSet(null, err)
    1
  }
} finally {
  Elide.close()
}.also {
  // exit the process if the global exit flag is set
  if (exitOnComplete) exitProcess(exitCode)
}
