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

package elide.runtime.gvm.jvm

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Locale
import javax.tools.*
import elide.annotations.API
import elide.runtime.diag.DiagnosticsContainer
import elide.runtime.diag.DiagnosticsReceiver
import elide.runtime.diag.DiagnosticsSuite
import elide.runtime.precompiler.Precompiler
import elide.runtime.precompiler.Precompiler.*

// Precompiler agent which is capable of translating Java source code to JVM bytecode.
@API public object JavaPrecompiler : BytecodePrecompiler<JavaCompilerConfig> {
  override fun invoke(req: PrecompileSourceRequest<JavaCompilerConfig>, input: String): ByteBuffer? {
    val compiler: JavaCompiler = ToolProvider.getSystemJavaCompiler()
    val diagnosticsReceiver = DiagnosticsAgent()
    val locale = Locale.getDefault()
    val fileManager = compiler.getStandardFileManager(diagnosticsReceiver, locale, StandardCharsets.UTF_8)
    val tmproot = Files.createTempDirectory("elide-jvm-precompile")
    val tmpfile = tmproot.resolve(req.source.name).toFile()

    tmpfile.deleteOnExit()
    tmpfile.writer(StandardCharsets.UTF_8).use {
      it.write(input)
    }
    val units = fileManager.getJavaFileObjects(tmpfile)
    compiler.getTask(
      null,
      fileManager,
      diagnosticsReceiver,
      null,
      null,
      units,
    ).let {
      when (it.call()) {
        true -> {} // nothing to do
        false -> error("Failed to compile Java source code")
      }
    }

    val outbytes = Files.readAllBytes(
      tmpfile.resolveSibling("${req.source.name.substringBefore('.')}.class").toPath()
    )
    return ByteBuffer.wrap(outbytes)
  }

// Receives diagnostics reported by `javac`, then translates and reports them according to Elide's record structure.
  private class DiagnosticsAgent(
    private val container: DiagnosticsContainer = DiagnosticsContainer.create()
  ) : DiagnosticListener<JavaFileObject>,
    DiagnosticsSuite by container,
    DiagnosticsReceiver by container {
// Proxy close calls to the underlying container.
    override fun close(): Unit = container.close()

    override fun report(diagnostic: Diagnostic<out JavaFileObject>?) {
      diagnostic ?: return
    }
  }

  // Provider for the precompiler.
  public class Provider : Precompiler.Provider<JavaPrecompiler> {
    override fun get(): JavaPrecompiler = JavaPrecompiler
  }
}
