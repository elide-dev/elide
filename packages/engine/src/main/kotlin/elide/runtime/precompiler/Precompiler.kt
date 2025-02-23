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
package elide.runtime.precompiler

import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.function.Supplier
import elide.runtime.diag.DiagnosticsContainer
import elide.runtime.diag.DiagnosticsSuite
import elide.runtime.precompiler.Precompiler.PrecompileSourceRequest

/**
 * # Precompiler
 *
 * Describes the concept of a "precompiler," which is used by Elide to transform input code material (either source code
 * or bytecode) into a form that is supported by the runtime or more efficiently consumed by the runtime.
 *
 * Precompilers are typically implemented to transform code written in a high-level language (e.g., TypeScript, Kotlin)
 * into code which is directly executable by a language engine (e.g. JavaScript, JVM bytecode).
 *
 * Two major types of precompilers are supported by Elide:
 *
 * - [SourcePrecompiler]: Responsible for transforming a [String] of source code into another [String] of source code.
 *   This is the base type for transformation cases like the TypeScript example above.
 *
 * - [BytecodePrecompiler]: Responsible for transforming a [String] of source code into a [ByteBuffer] of bytecode.
 *   This is the base type for transformation cases like the Kotlin example above.
 *
 * In all cases, precompiler steps run immediately after code is loaded and immediately before it is evaluated for
 * execution. Precompilers are only loaded once per run and are only loaded if needed.
 *
 * @param C Configuration type for this precompiler.
 * @param I Input type accepted by this precompiler.
 * @param O Output type produced by this precompiler.
 */
public sealed interface Precompiler<C, I, O> where C : Precompiler.Configuration {
  /**
   * Base interface for precompiled bundles of code.
   *
   * @property name Name of the bundle.
   * @property path Path to the bundle.
   */
  public interface BundleInfo {
    public val name: String
    public val path: Path
  }

  /**
   * Base interface for precompiler configurations.
   */
  public interface Configuration

  /**
   * Holds information about the source code being precompiled.
   *
   * @param name Name of the source code being precompiled.
   * @param path Path to the source code being precompiled.
   */
  @JvmRecord public data class PrecompileSourceInfo(
    val name: String,
    val path: Path? = null,
  )

  /**
   * Represents a request from the engine to precompile source code.
   *
   * @param source Information about the source to be compiled.
   * @param config Configuration for the precompiler.
   */
  public class PrecompileSourceRequest<C>(
    public val source: PrecompileSourceInfo,
    public val config: C,
  )

  /**
   * Precompile the input using this precompiler.
   *
   * This method is called by the runtime to precompile the input before it is executed.
   *
   * @param req Information about the source code being precompiled.
   * @param input Input to be transformed by this precompiler.
   * @return Output produced by this precompiler.
   * @throws PrecompilerException for non-fatal and fatal precompiler errors, warnings, or other diagnostics.
   */
  @Throws(PrecompilerException::class)
  public suspend fun precompile(req: PrecompileSourceRequest<C>, input: I): O? = invoke(req, input)

  /**
   * Invoke this precompiler, producing an output of shape [O] for an input of shape [I].
   *
   * This is the ultimate method implemented by each precompiler class.
   *
   * @param req Information about the source code being precompiled.
   * @param input Input to be transformed by this precompiler.
   * @return Output produced by this precompiler.
   */
  public operator fun invoke(req: PrecompileSourceRequest<C>, input: I): O?

  /**
   * ## Source Precompiler
   *
   * Accepts code in the form of a simple [String], and is expected to return transformed code in the form of a [String]
   * which is directly executable by the runtime.
   */
  public fun interface SourcePrecompiler<C : Configuration> : Precompiler<C, String, String>

  /**
   * ## Bytecode Precompiler
   *
   * Accepts code in the form of a simple [String], and is expected to return transformed code in the form of a buffer
   * containing raw bytecode which is directly executable by the runtime.
   */
  public fun interface BytecodePrecompiler<C : Configuration> : Precompiler<C, String, ByteBuffer>

  /**
   * ## Bundle Precompiler
   *
   * Accepts code in the form of a simple [String], and is expected to return transformed code in the form of a file
   * which acts as an executable bundle (for example, a JAR).
   */
  public fun interface BundlePrecompiler<C, B> : Precompiler<C, String, B>
    where C : Configuration,
          B : BundleInfo

  /**
   * Generic provider of a precompiler instance; used for service loading.
   */
  public interface Provider<T> : Supplier<T> where T : Precompiler<*, *, *>
}

/**
 * Precompile using the provided inputs,
 */
public suspend inline fun <C : Precompiler.Configuration, I, O> Precompiler<C, I, O>.precompileSafe(
  req: PrecompileSourceRequest<C>,
  code: I,
): Pair<DiagnosticsSuite, O?> = try {
  DiagnosticsContainer.create() to precompile(req, code)
} catch (e: PrecompilerNoticeWithOutput) {
  @Suppress("UNCHECKED_CAST")
  e.diagnostics to e.output as O
}
