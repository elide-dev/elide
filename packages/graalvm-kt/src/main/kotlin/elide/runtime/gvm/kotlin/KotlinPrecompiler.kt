/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.gvm.kotlin

import org.graalvm.polyglot.Source
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.destinationAsFile
import java.io.Closeable
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.LinkedList
import kotlinx.atomicfu.atomic
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.diag.*
import elide.runtime.plugins.kotlin.Kotlin
import elide.runtime.precompiler.Precompiler
import elide.runtime.precompiler.Precompiler.*
import elide.runtime.precompiler.PrecompilerError
import elide.runtime.precompiler.PrecompilerNotice
import elide.runtime.precompiler.PrecompilerNoticeWithOutput

/**
 * Create a [DiagnosticInfo] object from a Kotlin compiler message.
 *
 * @param sev Severity of the message.
 * @param msg Message text.
 * @param location Source location of the message.
 * @return A [DiagnosticInfo] object representing the message.
 */
public fun Diagnostic.Companion.fromKotlincDiagnostic(
  sev: CompilerMessageSeverity,
  msg: String,
  location: CompilerMessageSourceLocation?,
): DiagnosticInfo = object : DiagnosticInfo {
  override val position: SourceLocation? get() = location?.let {
    SourceLocation(location.line.toUInt(), location.column.toUInt())
  }

  // @TODO source reference
  override val message: String get() = msg
  override val severity: Severity get() = when (sev) {
    EXCEPTION,
    ERROR -> Severity.ERROR
    WARNING,
    FIXED_WARNING,
    STRONG_WARNING -> Severity.WARN
    INFO,
    LOGGING,
    OUTPUT -> Severity.INFO
  }
}

// Implements a precompiler which compiles Kotlin to Java bytecode.
public object KotlinPrecompiler : BundlePrecompiler<KotlinCompilerConfig, KotlinRunnable> {
  public class PrecompileKotlinRequest(
    source: SourceInfo,
    config: KotlinCompilerConfig,
    public val jarTarget: Path? = null,
  ): PrecompileSourceRequest<KotlinCompilerConfig>(source, config)

  private val kotlinVerbose by lazy {
    System.getProperty("elide.kotlin.verbose") == "true"
  }
  private val ktCompiler by lazy { K2JVMCompiler() }

  internal fun currentConfig(): KotlinCompilerConfig = KotlinCompilerConfig(
    apiVersion = ApiVersion.KOTLIN_2_1,
    languageVersion = LanguageVersion.KOTLIN_2_1,
  )

  // Convert a Kotlin source path to an entrypoint string.
  private fun entrypointFromKotlinPath(path: Path, paths: List<Path>): String? {
    val packageStatement = try {
      // find package statement
      path.toFile().bufferedReader(StandardCharsets.UTF_8).use {
        it.lineSequence()
          .firstOrNull { line -> line.startsWith("package ") }
          ?.removePrefix("package ")
          ?.removeSuffix(";")
          ?.trim()
      }
    } catch (e: IOException) {
      throw IOException("Failed to read first line of Kotlin source file at $path", e)
    }
    val filename = path.fileName.toString().removeSuffix(".kt")
    return when {
      // `package.path.MainKt`
      filename == "main" -> buildString {
        when (packageStatement) {
          null, "" -> append("MainKt") // no package statement, just `MainKt`
          else -> {
            append(packageStatement) // package statement, so `package.path.MainKt`
            append('.')
            append("MainKt")
          }
        }
      }

      // `package.path.NamedMainKt`
      filename.endsWith("main", ignoreCase = true) -> buildString {
        when (packageStatement) {
          null, "" -> {}
          else -> {
            append(packageStatement)
            append('.')
          }
        }
        val suffix = filename.lowercase().removeSuffix("main")
        append(suffix.replaceFirstChar { it.uppercase() })
        append("MainKt")
      }

      // assume it is a top-level `main`
      else -> if (paths.size > 1) null else buildString {
        when (packageStatement) {
          null, "" -> {} // no package statement, just `filenameKt`
          else -> {
            append(packageStatement)
            append('.')
          }
        }
        append(filename.replaceFirstChar { it.uppercase() })
        append("Kt")
      }
    }
  }

  // Try to infer a Kotlin entrypoint, or consume one from flags or project configuration; fail if we can't determine.
  private fun buildOrGuessEntrypointForKotlinEntry(srcs: List<Path>): String? {
    // if we are provided an entrypoint, prefer that
    // @TODO entrypoint from project
    // @TODO entrypoint from flags

    // if there is a file somewhere in our source set called `main.kt` (case-insensitive), then we will prefer that
    val mainKt = srcs.firstOrNull { it.fileName.toString().endsWith("main.kt", ignoreCase = true) }
    val mainFromProject: Path? = null
    val mainFromFlags: Path? = null

    val (ambiguity, mainSelected) = when {
      mainFromFlags != null -> false to mainFromFlags
      mainFromProject != null && mainKt != null && mainFromProject != mainKt -> true to mainFromProject
      mainFromProject != null -> false to mainFromProject
      mainKt != null -> false to mainKt
      srcs.size == 1 -> false to srcs.first() // single source file, use that
      else -> return null
    }
    if (ambiguity) {
      Logging.root().warn(
        buildString {
          appendLine("Multiple potential entrypoints for Kotlin precompiled source:")
          appendLine("- Project: ${mainFromProject?.fileName ?: "<none>"}")
          appendLine("- Flags: ${mainFromFlags?.fileName ?: "<none>"}")
          appendLine("- Source: ${mainKt?.fileName ?: "<none>"}")
        }
      )
    }
    return entrypointFromKotlinPath(mainSelected, srcs)
  }

  @OptIn(DelicateElideApi::class)
  @Suppress("TooGenericExceptionCaught")
  override fun invoke(req: PrecompileSourceRequest<KotlinCompilerConfig>, input: String): KotlinRunnable {
    val srcs = req.source.allSources().toList()
    val isKotlinScript = req.source.name.endsWith(".kts") || srcs.any { it.endsWith(".kts") }
    if (isKotlinScript) {
      // should only be provided with one input in this case
      check(srcs.size < 2) {
        "Kotlin script precompilation should be provided with a maximum of one source file"
      }

      // if we are running a kotlin script, wire together the runnable and script template, and return it in a deferred
      // form; such runs do not need the precompiler.
      return KotlinScriptCallable(name = req.source.name, path = srcs.firstOrNull()) { ctx ->
        // execute as a script
        ElideKotlinScriptExecutor.execute(
          ctx,
          Source.newBuilder(Kotlin.languageId, input, req.source.name).build(),
        )
      }
    }
    val tmproot = Files.createTempDirectory("elide-kt-precompile")
    val tmpfile = tmproot.resolve(req.source.name).toFile()
    val closeables = LinkedList<Closeable>()
    val effectiveOut = when (val jarTarget = (req as? PrecompileKotlinRequest)?.jarTarget) {
      null -> tmproot.resolve("ktjvm.jar").toFile()
      else -> jarTarget.toFile()
    }

    val kotlinRoot = System.getenv("KOTLIN_HOME")
      ?.takeIf { it.isNotEmpty() }
      ?.let { Paths.get(it) }
      ?: Paths.get(
        System.getProperty("user.home"),
        "elide",
        "resources",
        "kotlin",
      ).takeIf {
        it.exists()
      }

    val kotlinVersionRoot = if (kotlinRoot != null && !kotlinRoot.endsWith(KotlinLanguage.VERSION)) {
      kotlinRoot.resolve(KotlinLanguage.VERSION)
    } else {
      kotlinRoot
    }

    val javaToolchainHome = Paths.get(requireNotNull(
      System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
    ) {
      "No JAVA_HOME set; please set it to compile to JVM bytecode"
    })

    tmpfile.deleteOnExit()
    tmpfile.writer(StandardCharsets.UTF_8).use {
      it.write(input)
    }

    val diagnostics = DiagnosticsListener()
    val svcs = Services.EMPTY
    val kotlinMajorMinor = KotlinLanguage.VERSION.substringBeforeLast('.')
    val args = ktCompiler.createArguments().apply {
      destinationAsFile = effectiveOut
      disableStandardScript = true
      jdkHome = javaToolchainHome.absolutePathString()
      freeArgs = mutableListOf(tmpfile.absolutePath)
      apiVersion = kotlinMajorMinor
      languageVersion = kotlinMajorMinor
      useFirLT = true
      kotlinVersionRoot?.absolutePathString()?.let {
        kotlinHome = it
      }
    }
    try {
      ktCompiler.exec(diagnostics, svcs, args)
    } catch (rxe: RuntimeException) {
      throw PrecompilerError("Failed to precompile Kotlin", rxe)
    } finally {
      closeables.forEach { it.close() }
    }
    return KotlinJarBundleInfo(
      name = req.source.name,
      path = effectiveOut.toPath(),
      entrypoint = buildOrGuessEntrypointForKotlinEntry(srcs),
    ).also {
      if (diagnostics.hasErrors()) {
        when (diagnostics.fatal) {
          true -> throw PrecompilerNotice.from(diagnostics)
          false -> throw PrecompilerNoticeWithOutput.from(diagnostics, req.source)
        }
      }
    }
  }

  /** Report diagnostics from Kotlin's compiler. */
  private class DiagnosticsListener(
    private var container: DiagnosticsContainer = DiagnosticsContainer.create()
  ) : MessageCollector,
    DiagnosticsSuite by container,
    DiagnosticsReceiver by container {
    // Whether this listener has seen errors.
    private val errorsSeen = atomic(false)

    // Close the underlying container.
    override fun close(): Unit = container.close()

    override fun clear() {
      container = DiagnosticsContainer.create()
    }

    override fun hasErrors(): Boolean = errorsSeen.value

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
      if (kotlinVerbose) {
        val msg = "[kotlinc] $severity: $message"
        when {
          severity.isError -> Logging.root().error(msg)
          severity.isWarning -> Logging.root().warn(msg)
          else -> Logging.root().info(msg)
        }
      }
      if (severity == ERROR || severity == EXCEPTION) {
        errorsSeen.value = true
      }
      container.report(Diagnostic.fromKotlincDiagnostic(severity, message, location))
    }
  }

  // Service-loader provider for the Kotlin precompiler.
  public class Provider : Precompiler.Provider<KotlinPrecompiler> {
    override fun get(): KotlinPrecompiler = KotlinPrecompiler
  }
}
