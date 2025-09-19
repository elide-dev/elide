package dev.elide.secrets

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInputPassword
import com.github.kinquirer.components.promptListObject
import dev.elide.secrets.Utils.choices
import dev.elide.secrets.dto.persisted.EncryptionMode

/** @author Lauri Heino <datafox> */
internal object Prompts {
  fun passphrase(): String {
    checkInteractive()
    for (i in 0 until Values.INVALID_PASSPHRASE_TRIES) {
      val pass = KInquirer.promptInputPassword(Values.ENTER_PASSPHRASE_PROMPT)
      val repeat = KInquirer.promptInputPassword(Values.ENTER_PASSPHRASE_REPEAT_PROMPT)
      if (pass == repeat) return pass
      println(Values.PASSPHRASES_NOT_IDENTICAL_MESSAGE)
    }
    throw IllegalArgumentException(Values.MISMATCHING_PASSPHRASES_EXCEPTION)
  }

  fun localUserKeyMode(): EncryptionMode {
    checkInteractive()
    return KInquirer.promptListObject(
      Values.LOCAL_STORAGE_ENCRYPTION_PROMPT,
      EncryptionMode.entries.choices { displayName },
    )
  }

  fun validateLocalPassphrase(validator: (String) -> Boolean): String {
    checkInteractive()
    for (i in 0 until Values.INVALID_PASSPHRASE_TRIES) {
      val pass = KInquirer.promptInputPassword(Values.ENTER_PASSPHRASE_PROMPT)
      if (validator(pass)) return pass
      println(Values.INVALID_PASSPHRASE_MESSAGE)
    }
    throw IllegalArgumentException(Values.INVALID_PASSPHRASE_EXCEPTION)
  }

  fun superKeyMode(): EncryptionMode {
    checkInteractive()
    println(Values.SUPER_ACCESS_ENCRYPTION_MESSAGE)
    return KInquirer.promptListObject(
      Values.GENERIC_CHOICE_PROMPT,
      EncryptionMode.entries.choices { displayName },
    )
  }

  fun accessMode(): EncryptionMode {
    checkInteractive()
    return KInquirer.promptListObject(
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
