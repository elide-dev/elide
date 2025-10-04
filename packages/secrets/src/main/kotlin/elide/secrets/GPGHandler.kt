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

import java.io.OutputStream
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString

/**
 * Runner for the `gpg` binary.
 *
 * @author Lauri Heino <datafox>
 */
internal object GPGHandler {
  fun gpgKeys(): Map<String, String> = gpgKeys(false)

  fun gpgPrivateKeys(): Map<String, String> = gpgKeys(true)

  fun runGPG(input: ByteString?, vararg params: String): ByteString {
    val process = ProcessBuilder("gpg", *params).start()
    if (input != null) process.outputStream.use<OutputStream, Unit> { it.write(input.toByteArray()) }
    process.waitFor()
    return ByteString(process.inputStream.use { it.readAllBytes() })
  }

  private fun gpgKeys(private: Boolean): Map<String, String> {
    val lines = runGPG(null, if (private) "-K" else "-k").decodeToString().lines()
    val keyLines: MutableList<List<String>> = mutableListOf()
    var list: MutableList<String>? = null
    for (line in lines) {
      if (line.startsWith(if (private) "sec" else "pub")) {
        list?.let { keyLines.add(it) }
        list = if (line.contains(Regex("revoked|expired"))) null else mutableListOf()
      }
      list?.add(line)
    }
    list?.let { keyLines.add(it) }
    return keyLines.associate { it[2].substringAfter("] ") to it[1].trim().lowercase() }
  }
}
