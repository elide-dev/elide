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

import com.oracle.truffle.api.CompilerAsserts
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal
import com.oracle.truffle.api.TruffleLanguage
import com.oracle.truffle.api.TruffleLanguage.*
import com.oracle.truffle.api.nodes.LanguageInfo
import com.oracle.truffle.espresso.EspressoLanguage
import com.oracle.truffle.espresso.runtime.EspressoContext
import org.graalvm.polyglot.SandboxPolicy
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import kotlin.concurrent.Volatile
import elide.runtime.gvm.kotlin.feature.KotlinResource

private const val KOTLIN_ID = "kotlin"
private const val KOTLIN_NAME = "Kotlin"
private const val KOTLIN_IMPL = "Kotlin/JVM (Espresso)"
private const val KOTLIN_IMPL_VERSION = "2.2.20"
private const val KOTLIN_MIME_TYPE = "application/x-kotlin"

/**
 * Kotlin facade language definition for GraalVM; defines mappings, configuration, constants, and other Kotlin-specific
 * material on top of Espresso.
 */
@Registration(
  id = KOTLIN_ID,
  name = KOTLIN_NAME,
  implementationName = KOTLIN_IMPL,
  version = KOTLIN_IMPL_VERSION,
  dependentLanguages = ["java"],
  defaultMimeType = KOTLIN_MIME_TYPE,
  website = "https://docs.elide.dev",
  fileTypeDetectors = [KotlinFileTypeDetector::class],
  internalResources = [KotlinResource::class],
  contextPolicy = ContextPolicy.SHARED,
  sandbox = SandboxPolicy.TRUSTED,
  characterMimeTypes = [
    KOTLIN_MIME_TYPE,
  ],
)
public class KotlinLanguage : TruffleLanguage<EspressoContext>() {
  @CompilationFinal @Volatile
  private lateinit var ktCompiler: K2JVMCompiler

  override fun createContext(currentEnv: Env): EspressoContext {
    CompilerAsserts.neverPartOfCompilation()
    val javaInfo: LanguageInfo = requireNotNull(currentEnv.internalLanguages["java"]) {
      "Failed to initialize Espresso; required for Kotlin. Crashing!"
    }
    currentEnv.initializeLanguage(javaInfo)
    if (!this::ktCompiler.isInitialized) {
      ktCompiler = K2JVMCompiler()
    }
    return EspressoContext.get(null)
  }

  @Suppress("UNUSED", "UnusedPrivateProperty") public companion object {
    public const val ID: String = KOTLIN_ID
    public const val VERSION: String = KOTLIN_IMPL_VERSION
    public const val COROUTINES_VERSION: String = "1.10.2"
    public const val SERIALIZATION_VERSION: String = "1.9.0"

    @JvmStatic private val ESPRESSO: LanguageReference<EspressoLanguage> = LanguageReference.create(
      EspressoLanguage::class.java,
    )

    @JvmStatic private val REFERENCE: LanguageReference<KotlinLanguage> = LanguageReference.create(
      KotlinLanguage::class.java,
    )
  }
}
