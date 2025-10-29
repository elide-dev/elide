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
package elide.secrets

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest

/**
 * Common methods for tests.
 *
 * @author Lauri Heino <datafox>
 */
abstract class AbstractSecretTest {
  @BeforeTest
  fun `reset state`() {
    SecretsState.reset()
  }

  protected val secretPass = "sus"

  protected val secretFiles =
    listOf("secret/local.db", "secret/metadata.json", "secret/secrets-test.db", "secret/secrets-test.key")

  protected val secretTypes = mapOf("test" to SecretType.TEXT, "env" to SecretType.TEXT, "binary" to SecretType.BINARY)

  protected inline fun withTemp(crossinline op: suspend (Path) -> Unit) = runTest {
    val temp =
      Files.createTempDirectory(
        Path.of(System.getProperty("java.io.tmpdir")),
        "elide-test-",
      )
    val fileOf = temp.toFile()
    var didError = false
    try {
      op(temp)
    } catch (ioe: Throwable) {
      didError = true
      throw ioe
    } finally {
      if (!didError) fileOf.deleteRecursively()
    }
  }

  protected fun copyFiles(target: Path, files: List<String>) {
    files.forEach {
      Files.copy(SecretsTest::class.java.getResourceAsStream(it)!!, target.resolve(it.substringAfterLast("/")))
    }
  }
}
