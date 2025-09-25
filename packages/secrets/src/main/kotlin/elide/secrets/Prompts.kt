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

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInputPassword
import com.github.kinquirer.components.promptListObject
import elide.secrets.Utils.choices
import elide.secrets.dto.persisted.EncryptionMode

/** @author Lauri Heino <datafox> */
internal object Prompts {
  fun passphrase(prompts: MutableList<String>): String {
    checkInteractive()
    @Suppress("unused")
    for (i in 0 until Values.INVALID_PASSPHRASE_TRIES) {
      val pass = prompts.removeFirstOrNull() ?: KInquirer.promptInputPassword(Values.ENTER_PASSPHRASE_PROMPT)
      val repeat = prompts.removeFirstOrNull() ?: KInquirer.promptInputPassword(Values.ENTER_PASSPHRASE_REPEAT_PROMPT)
      if (pass == repeat) return pass
      println(Values.PASSPHRASES_NOT_IDENTICAL_MESSAGE)
    }
    throw IllegalArgumentException(Values.MISMATCHING_PASSPHRASES_EXCEPTION)
  }

  fun localUserKeyMode(prompts: MutableList<String>): EncryptionMode {
    checkInteractive()
    return prompts.removeFirstOrNull()?.let { EncryptionMode.valueOf(it) }
      ?: KInquirer.promptListObject(
        Values.LOCAL_STORAGE_ENCRYPTION_PROMPT,
        EncryptionMode.entries.choices { displayName },
      )
  }

  fun validateLocalPassphrase(prompts: MutableList<String>, validator: (String) -> Boolean): String {
    checkInteractive()
    @Suppress("unused")
    for (i in 0 until Values.INVALID_PASSPHRASE_TRIES) {
      val pass = prompts.removeFirstOrNull() ?: KInquirer.promptInputPassword(Values.ENTER_PASSPHRASE_PROMPT)
      if (validator(pass)) return pass
      println(Values.INVALID_PASSPHRASE_MESSAGE)
    }
    throw IllegalArgumentException(Values.INVALID_PASSPHRASE_EXCEPTION)
  }

  fun superKeyMode(prompts: MutableList<String>): EncryptionMode {
    checkInteractive()
    println(Values.SUPER_ACCESS_ENCRYPTION_MESSAGE)
    return prompts.removeFirstOrNull()?.let { EncryptionMode.valueOf(it) }
      ?: KInquirer.promptListObject(
        Values.GENERIC_CHOICE_PROMPT,
        EncryptionMode.entries.choices { displayName },
      )
  }

  fun accessMode(prompts: MutableList<String>): EncryptionMode {
    checkInteractive()
    return prompts.removeFirstOrNull()?.let { EncryptionMode.valueOf(it) }
      ?: KInquirer.promptListObject(
        Values.ACCESS_FILE_ENCRYPTION_PROMPT,
        EncryptionMode.entries.choices { displayName },
      )
  }

  fun gpgPrivateKey(): String {
    checkInteractive()
    val keys = GPGHandler.gpgPrivateKeys()
    return KInquirer.promptListObject(Values.GPG_PRIVATE_KEY_PROMPT, keys.choices { "$it (${substring(0, 8)})" })
  }

  fun gpgPublicKey(): String {
    checkInteractive()
    val keys = GPGHandler.gpgKeys()
    return KInquirer.promptListObject(Values.GPG_PUBLIC_KEY_PROMPT, keys.choices { "$it (${substring(0, 8)})" })
  }

  private fun checkInteractive() {
    if (!SecretsState.interactive) throw IllegalStateException(Values.NOT_IN_INTERACTIVE_MODE_EXCEPTION)
  }
}
