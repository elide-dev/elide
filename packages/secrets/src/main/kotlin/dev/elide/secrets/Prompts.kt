package dev.elide.secrets

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInputPassword
import com.github.kinquirer.components.promptListObject
import com.github.kinquirer.core.Choice
import dev.elide.secrets.dto.persisted.EncryptionMode

/** @author Lauri Heino <datafox> */
internal object Prompts {
  fun passphrase(): String {
    checkInteractive()
    for (i in 0 until Values.INVALID_PASSPHRASE_TRIES) {
      val pass = KInquirer.promptInputPassword("Please enter your passphrase:")
      val repeat = KInquirer.promptInputPassword("Please enter your passphrase again:")
      if (pass == repeat) return pass
      println("Passphrases were not identical")
    }
    throw IllegalArgumentException("Mismatching passphrases entered too many times")
  }

  fun localUserKeyMode(): EncryptionMode {
    checkInteractive()
    return KInquirer.promptListObject(
      "How do you want to encrypt locally stored secrets?",
      EncryptionMode.entries.map { Choice(it.displayName, it) },
    )
  }

  fun validateLocalPassphrase(validator: (String) -> Boolean): String {
    checkInteractive()
    for (i in 0 until Values.INVALID_PASSPHRASE_TRIES) {
      val pass = KInquirer.promptInputPassword("Please enter your passphrase:")
      if (validator(pass)) return pass
      println("Passphrases was invalid")
    }
    throw IllegalArgumentException("Invalid passphrase entered too many times")
  }

  fun superKeyMode(): EncryptionMode {
    checkInteractive()
    return KInquirer.promptListObject(
      "How do you want to encrypt the remote super access file? This file will be able to decrypt all secrets!",
      EncryptionMode.entries.map { Choice(it.displayName, it) },
    )
  }

  fun accessMode(): EncryptionMode {
    checkInteractive()
    return KInquirer.promptListObject(
      "How do you want to encrypt this access file?",
      EncryptionMode.entries.map { Choice(it.displayName, it) },
    )
  }

  fun gpgPrivateKey(): String {
    checkInteractive()
    val keys = GPGHandler.gpgPrivateKeys()
    return KInquirer.promptListObject(
      "Please select a private key",
      keys.map { (name, fingerprint) -> Choice("$name (${fingerprint.substring(0, 8)})", fingerprint) },
    )
  }

  fun gpgPublicKey(): String {
    checkInteractive()
    val keys = GPGHandler.gpgKeys()
    return KInquirer.promptListObject(
      "Please select a public key",
      keys.map { (name, fingerprint) -> Choice("$name (${fingerprint.substring(0, 8)})", fingerprint) },
    )
  }

  private fun checkInteractive() {
    if (!SecretsState.interactive) throw IllegalStateException("Not in interactive mode")
  }
}
