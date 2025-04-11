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
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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

// Implements a precompiler which compiles Kotlin to Java bytecode.
public object KotlinPrecompiler : BundlePrecompiler<KotlinCompilerConfig, KotlinRunnable> {
  // Embedded Kotlin version.
  private const val KOTLIN_VERSION = "2.1.20"
  private val kotlinVerbose by lazy {
    System.getProperty("elide.kotlin.verbose") == "true"
  }
  private val ktCompiler by lazy { K2JVMCompiler() }

  internal fun currentConfig(): KotlinCompilerConfig = KotlinCompilerConfig(
    apiVersion = ApiVersion.KOTLIN_2_1,
    languageVersion = LanguageVersion.KOTLIN_2_1,
  )

  private fun Diagnostic.Companion.from(
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
      STRONG_WARNING -> Severity.WARN
      INFO,
      LOGGING,
      OUTPUT -> Severity.INFO
    }
  }

  @OptIn(DelicateElideApi::class)
  @Suppress("TooGenericExceptionCaught")
  override fun invoke(req: PrecompileSourceRequest<KotlinCompilerConfig>, input: String): KotlinRunnable {
    val isKotlinScript = req.source.path?.endsWith(".kts") == true || req.source.name.endsWith(".kts")
    if (isKotlinScript) {
      // if we are running a kotlin script, wire together the runnable and script template, and return it in a deferred
      // form; such runs do not need the precompiler.
      return KotlinScriptCallable(name = req.source.name, path = req.source.path) { ctx ->
        // execute as a script
        ElideKotlinScriptExecutor.execute(
          ctx,
          Source.newBuilder(Kotlin.languageId, input, req.source.name).build(),
        )
      }
    }
    val closeables = LinkedList<Closeable>()
    val tmproot = Files.createTempDirectory("elide-kt-precompile")
    val tmpfile = tmproot.resolve(req.source.name).toFile()
    val jarfile = tmproot.resolve("ktjvm.jar").toFile()
    val kotlinRoot = System.getenv("KOTLIN_HOME")
      ?.takeIf { it.isNotEmpty() }
      ?.let { Paths.get(it) }
      ?: Paths.get(
        System.getProperty("user.home"),
        "elide",
        "kotlin",
        "kotlin-home",
      ).takeIf {
        it.exists()
      }
    val kotlinVersionRoot = kotlinRoot
      ?.resolve(KOTLIN_VERSION)

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
    val kotlinMajorMinor = KOTLIN_VERSION.substringBeforeLast('.')
    val args = ktCompiler.createArguments().apply {
      destinationAsFile = jarfile
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
      path = jarfile.toPath(),
    ).also {
      if (diagnostics.hasErrors()) {
        when (diagnostics.fatal) {
          true -> throw PrecompilerNotice.from(diagnostics)
          false -> throw PrecompilerNoticeWithOutput.from(diagnostics, req.source)
        }
      }
    }
  }

  // Report diagnostics from Kotlin's compiler.
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
      container.report(Diagnostic.from(severity, message, location))
    }
  }

  // Service-loader provider for the Kotlin precompiler.
  public class Provider : Precompiler.Provider<KotlinPrecompiler> {
    override fun get(): KotlinPrecompiler = KotlinPrecompiler
  }
}
