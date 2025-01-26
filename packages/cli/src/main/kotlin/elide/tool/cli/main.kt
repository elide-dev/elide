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

package elide.tool.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parsers.CommandLineParser
import com.github.ajalt.clikt.parsers.flatten
import com.jakewharton.mosaic.runMosaic
import dev.elide.cli.bridge.CliNativeBridge
import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.context.ApplicationContext
import picocli.CommandLine
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess
import elide.annotations.Eager

// Whether to enable Mosaic and Compose.
private val enableMosaic = System.getProperty("elide.mosaic").toBoolean()

// Whether to enable the experimental V2 entrypoint through Clikt.
private val ENABLE_CLI_ENTRY_V2 = System.getenv("ELIDE_EXPERIMENTAL")?.ifBlank { null } != null

// Whether to exist after completion.
val exitOnComplete: AtomicBoolean = AtomicBoolean(true)

// Exit code for this run.
val exitCode: AtomicInteger = AtomicInteger(0)

// Unhandled error that caused exit, if any.
val unhandledExc: AtomicReference<Throwable> = AtomicReference(null)

// Read the old entrypoint factory.
private fun sorryIHaveToFactory(args: Array<String>): CommandLine = ApplicationContext
  .builder()
  .eagerInitAnnotated(Eager::class.java)
  .args(*args)
  .start().use { CommandLine(Elide::class.java, MicronautFactory(it)) }

// Run the given function, optionally using Mosaic (if enabled).
private suspend inline fun runEntry(crossinline fn: suspend () -> Unit) = when (enableMosaic) {
  true -> runMosaic { fn() }
  false -> fn()
}

// Run the Clikt or regular entrypoint.
private suspend inline fun runInner(args: Array<String>): Int = when (ENABLE_CLI_ENTRY_V2) {
  false -> Elide.entry(args)
  true -> ApplicationContext
    .builder()
    .eagerInitAnnotated(Eager::class.java)
    .args(*args).start().use { applicationContext ->
      MicronautFactory(applicationContext).use { factory ->
        runCatching {
          val procInfo = ProcessHandle.current().info()
          val cmd = procInfo.command().orElse("elide")

          Elide.installStatics(
            cmd,
            args,
            System.getProperty("user.dir"),
          )
          CommandLineParser
            .parse(factory.create(Elide::class.java), args.toList().also { Statics.args.set(it) })
        }.onFailure {
          println("Failed to parse arguments: ${it.message}")
          it.printStackTrace()
        }.getOrNull()?.let { command ->
          try {
            val cmd = command.invocation
              .flatten()
              .first()
              .command

            cmd.enter().exitCode
          } catch (err: PrintHelpMessage) {
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

// Perform early startup initialization tasks.
private fun initialize() {
  CliNativeBridge.initialize()
}

/**
 * Main entrypoint for Elide on the command line.
 *
 * This entrypoint method will call [exitProcess] with the exit code of the program; for testing or API-based dispatch,
 * see other entrypoints.
 *
 * @param args Arguments to run with.
 */
suspend fun main(args: Array<String>): Unit = try {
  // perform early init
  initialize()

  // run the entrypoint
  runEntry {
    exitCode.set(try {
      runInner(args)
    } catch (err: Throwable) {
      unhandledExc.compareAndSet(null, err)
      1
    })
  }
} finally {
  Elide.close()
}.also {
  // exit the process if the global exit flag is set
  if (exitOnComplete.get()) exitProcess(exitCode.get())
}
