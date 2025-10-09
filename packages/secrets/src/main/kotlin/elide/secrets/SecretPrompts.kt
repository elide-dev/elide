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
import elide.secrets.SecretUtils.choices
import elide.secrets.dto.persisted.EncryptionMode

/**
 * Tools around [KInquirer] prompts.
 *
 * @author Lauri Heino <datafox>
 */
internal object SecretPrompts {
  fun passphrase(prompts: MutableList<String>): String {
    checkInteractive()
    @Suppress("unused")
    for (i in 0 until SecretValues.INVALID_PASSPHRASE_TRIES) {
      val pass = prompts.removeFirstOrNull() ?: KInquirer.promptInputPassword(SecretValues.ENTER_PASSPHRASE_PROMPT)
      val repeat =
        prompts.removeFirstOrNull() ?: KInquirer.promptInputPassword(SecretValues.ENTER_PASSPHRASE_REPEAT_PROMPT)
      if (pass == repeat) return pass
      println(SecretValues.PASSPHRASES_NOT_IDENTICAL_MESSAGE)
    }
    throw IllegalArgumentException(SecretValues.MISMATCHING_PASSPHRASES_EXCEPTION)
  }

  fun localUserKeyMode(prompts: MutableList<String>): EncryptionMode {
    checkInteractive()
    return prompts.removeFirstOrNull()?.let { EncryptionMode.valueOf(it) }
      ?: KInquirer.promptListObject(
        SecretValues.LOCAL_STORAGE_ENCRYPTION_PROMPT,
        EncryptionMode.entries.choices { displayName },
      )
  }

  fun validateLocalPassphrase(prompts: MutableList<String>, validator: (String) -> Boolean): String {
    checkInteractive()
    @Suppress("unused")
    for (i in 0 until SecretValues.INVALID_PASSPHRASE_TRIES) {
      val pass = prompts.removeFirstOrNull() ?: KInquirer.promptInputPassword(SecretValues.ENTER_PASSPHRASE_PROMPT)
      if (validator(pass)) return pass
      println(SecretValues.INVALID_PASSPHRASE_MESSAGE)
    }
    throw IllegalArgumentException(SecretValues.INVALID_PASSPHRASE_EXCEPTION)
  }

  fun superKeyMode(prompts: MutableList<String>): EncryptionMode {
    checkInteractive()
    println(SecretValues.SUPER_ACCESS_ENCRYPTION_MESSAGE)
    return prompts.removeFirstOrNull()?.let { EncryptionMode.valueOf(it) }
      ?: KInquirer.promptListObject(
        SecretValues.GENERIC_CHOICE_PROMPT,
        EncryptionMode.entries.choices { displayName },
      )
  }

  fun accessMode(prompts: MutableList<String>): EncryptionMode {
    checkInteractive()
    return prompts.removeFirstOrNull()?.let { EncryptionMode.valueOf(it) }
      ?: KInquirer.promptListObject(
        SecretValues.ACCESS_FILE_ENCRYPTION_PROMPT,
        EncryptionMode.entries.choices { displayName },
      )
  }

  fun gpgPrivateKey(): String {
    checkInteractive()
    val keys = GPGHandler.gpgPrivateKeys()
    return KInquirer.promptListObject(SecretValues.GPG_PRIVATE_KEY_PROMPT, keys.choices { "$it (${substring(0, 8)})" })
  }

  fun gpgPublicKey(): String {
    checkInteractive()
    val keys = GPGHandler.gpgKeys()
    return KInquirer.promptListObject(SecretValues.GPG_PUBLIC_KEY_PROMPT, keys.choices { "$it (${substring(0, 8)})" })
  }

  private fun checkInteractive() {
    if (!SecretsState.interactive) throw IllegalStateException(SecretValues.NOT_IN_INTERACTIVE_MODE_EXCEPTION)
  }
}
