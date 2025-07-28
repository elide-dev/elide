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
package elide.tooling.kotlin

import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSNode
import java.util.ServiceLoader
import kotlinx.collections.immutable.toImmutableList
import elide.exec.Result
import elide.exec.asExecResult
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tooling.Classpath
import elide.tooling.jvm.JavaCompiler
import com.google.devtools.ksp.impl.KotlinSymbolProcessing as KSP

/**
 * # Kotlin Symbol Processing
 *
 * Implements support for KSP (Kotlin Symbol Processing), with built-in processors; as a general limitation of SVM,
 * Elide can only run processors available on the build-time classpath (this limitation may change in the future). Elide
 * ships with several processors.
 */
public object KotlinSymbolProcessing {
  // Logger to use.
  private val logging by lazy { Logging.of(KotlinSymbolProcessing::class) }

  // Built-in KSP processors, assembled from the built-time classpath.
  private val builtinProcessors by lazy {
    ServiceLoader.load(SymbolProcessorProvider::class.java, ClassLoader.getSystemClassLoader()).toImmutableList()
  }

  // Built-in processors to use for tests, assembled from the built-time classpath.
  private val builtinTestProcessors by lazy {
    ServiceLoader.load(SymbolProcessorProvider::class.java, ClassLoader.getSystemClassLoader()).toImmutableList()
      .filter { isTestProcessor(it) }
  }

  // Determine whether a processor is eligible for running against test sources.
  @Suppress("UNUSED_PARAMETER", "FunctionOnlyReturningConstant")
  private fun isTestProcessor(it: SymbolProcessorProvider): Boolean = true

  // Logging facade created for each KSP run.
  private class LoggerFacade(private val logging: Logger) : KSPLogger {
    private fun buildMessage(message: String, symbol: KSNode?): String = buildString {
      append(message)
      if (symbol != null) append(" (at ${symbol.location})")
    }

    override fun logging(message: String, symbol: KSNode?) = logging.debug { buildMessage(message, symbol) }
    override fun info(message: String, symbol: KSNode?) = logging.info { buildMessage(message, symbol) }
    override fun warn(message: String, symbol: KSNode?) = logging.warn { buildMessage(message, symbol) }
    override fun error(message: String, symbol: KSNode?) = logging.error { buildMessage(message, symbol) }
    override fun exception(e: Throwable) = logging.error("KSP error", e)
  }

  /**
   * Create a logging facade, which either uses the provided [facade] as a delegate, or the main [logging] pipe for the
   * KSP tooling.
   *
   * @param facade Facade to use as a delegate, if any.
   * @return KSP-compliant logger.
   */
  @JvmStatic public fun loggerFacade(facade: Logger? = null): KSPLogger = LoggerFacade(facade ?: logging)

  /**
   * Return the full suite of build-time-visible processors.
   *
   * @return Processors visible at build time.
   */
  @JvmStatic public fun allProcessors(test: Boolean = false): Sequence<SymbolProcessorProvider> = when (test) {
    false -> builtinProcessors
    true -> builtinTestProcessors
  }.asSequence()

  /**
   * Run all supported built-in KSP processors on the provided [classpath], building arguments using the provided block
   * at [argsBuilder].
   *
   * @param classpath Classpath to scan for subject class material.
   * @param argsBuilder Block to build arguments for KSP.
   */
  @JvmStatic public fun processSymbols(classpath: Classpath, logger: Logger? = null, argsBuilder: KspActor): Result {
    logging.debug { "Starting KSP run with classpath of size '${classpath.size}'" }
    if (logging.isTraceEnabled) logging.trace { "Classpath for processing: ${classpath.toList()}" }

    logging.trace { "Resolving JAVA_HOME for KSP" }
    val javaToolchainHome = JavaCompiler.resolveJavaToolchain(kotlinc)
    logging.debug { "KSP is using Java toolchain at: '$javaToolchainHome'" }

    logging.trace { "Building KSP configuration" }
    val kspConfig = KSPJvmConfig.Builder().apply {
      // prepare stock args
      jdkHome = javaToolchainHome.toFile()
    }.also {
      logging.trace { "Stock KSP configuration: $it" }
    }.apply { argsBuilder(classpath) }.build().also {
      logging.debug { "Finalized KSP configuration: $it" }
    }
    return KSP(kspConfig, builtinProcessors, loggerFacade(logger)).let { ksp ->
      runCatching {
        when (val exitCode = ksp.execute()) {
          KSP.ExitCode.OK -> logging.debug { "KSP exited with OK" }

          KSP.ExitCode.PROCESSING_ERROR -> {
            logging.error { "KSP exited with processing error (code: $exitCode)" }
            error("KSP failed")
          }
        }
      }.asExecResult()
    }
  }
}
