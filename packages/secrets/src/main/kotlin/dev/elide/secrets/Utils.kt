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
package dev.elide.secrets

import dev.elide.secrets.Utils.confirm
import dev.elide.secrets.Utils.options
import dev.elide.secrets.dto.persisted.SecretMetadata
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.io.bytestring.ByteString

/**
 * Internal utilities for secrets.
 *
 * @author Lauri Heino <datafox>
 */
internal object Utils {
  /** Generates [size] bytes. */
  fun generateBytes(size: Int): ByteString = ByteString(ByteArray(size).apply { SecureRandom().nextBytes(this) })

  /**
   * Lists strings from [options] to [console's][console] output, then reads user's choice from input and invokes the
   * selected option's lambda. Loops if invalid input is given.
   */
  fun options(console: Console, options: List<Pair<String, () -> Unit>>) {
    while (true) {
      console.println("Options: ")
      options.forEachIndexed { index, (prompt, _) -> console.println("  ${index + 1}: $prompt") }
      console.print("Please select an option: ")
      val result: String = console.readln().trim()
      val choice = result.toIntOrNull()
      if (choice == null || choice < 1 || choice > options.size) console.println("Invalid option '$result'")
      else {
        options[choice - 1].second()
        return
      }
    }
  }

  /**
   * Prints a yes/no confirmation prompt to [console] and reads user's choice and returns a boolean. Loops if invalid
   * input is given.
   */
  fun confirm(console: Console, confirmation: String): Boolean {
    while (true) {
      console.print("$confirmation [y/n]: ")
      when (console.readln().trim()) {
        "y" -> return true
        "n" -> return false
      }
    }
  }

  /**
   * Reads an input from [console], then [confirms][confirm] that the correct input was given. Loops until [confirm]
   * returns `true`.
   */
  fun readWithConfirm(
    console: Console,
    prompt: String,
    prefix: String = "Is \"",
    suffix: String = "\" correct?",
  ): String {
    while (true) {
      console.print(prompt)
      val input: String = console.readln()
      if (confirm(console, "$prefix$input$suffix")) return input
    }
  }

  /** Returns the file name of a collection for [profile]. */
  fun collectionName(profile: String): String = "${name(profile)}${Values.COLLECTION_FILE_EXTENSION}"

  /** Returns the file name of a key for [profile]. */
  fun keyName(profile: String): String = "${name(profile)}${Values.KEY_FILE_EXTENSION}"

  /** Returns the file name of the files for a [profile] without an extension. */
  fun name(profile: String): String = "${Values.FILE_NAME_PREFIX}${Values.PROFILE_SEPARATOR}$profile"

  /** Calculates the GitHub-specific SHA-1 hash of [data]. */
  @OptIn(ExperimentalStdlibApi::class)
  fun sha(data: ByteString): String =
    MessageDigest.getInstance("SHA-1")
      .digest("blob ${data.size}\u0000".encodeToByteArray() + data.toByteArray())
      .toHexString()

  /**
   * Reads a passphrase from [console] twice and checks if they are identical. Returns the passphrase if they are,
   * looping otherwise.
   */
  fun passphrase(console: Console, prompt: String, repeatPrompt: String, invalid: String): String {
    while (true) {
      console.print(prompt)
      val entry: String = console.readPassword()
      console.print(repeatPrompt)
      val repeat: String = console.readPassword()
      if (entry == repeat) {
        return entry
      }
      console.println(invalid)
    }
  }

  /** Throws an [IllegalArgumentException] if a profile name is invalid (is empty or contains whitespace). */
  fun checkName(name: String, type: String) {
    if (name.isEmpty() || ' ' in name) throw IllegalArgumentException("$type name must not be empty or contain spaces")
  }

  /**
   * Checks if [initialPassphrase] is valid by calling [decrypt]. If it returns an [O], a [Pair] is returned that
   * contains the passphrase and the returned [O]. If it returns `null` and [interactive] is `true`, reads a new [P]
   * with [readPassphrase] up to [Values.INVALID_PASSPHRASE_TRIES] times, checking each one with [decrypt] and returning
   * a pair of [P] and [O] if not `null`, and throws an [IllegalArgumentException] if the [P] was invalid too many
   * times. If [interactive] is `false` and the initial [decrypt] returns `null`, an [IllegalStateException] is thrown.
   */
  suspend fun <P, O> checkPassphrase(
    console: Console,
    interactive: Boolean,
    initialPassphrase: P,
    name: String,
    readPassphrase: suspend () -> P,
    decrypt: suspend (P) -> O?,
  ): Pair<P, O> {
    var tries = Values.INVALID_PASSPHRASE_TRIES
    var passphrase = initialPassphrase
    while (tries > 0) {
      tries--
      val out = decrypt(passphrase)
      if (out != null) return Pair(passphrase, out)
      if (!interactive) throw IllegalStateException("Invalid $name was given and interactive mode is off")
      console.println("Invalid $name")
      passphrase = readPassphrase()
    }
    return decrypt(passphrase)?.let { Pair(passphrase, it) }
      ?: throw IllegalArgumentException("Invalid $name entered too many times")
  }

  /**
   * Checks if [initialPassphrase] is valid by calling [DataHandler.validate]. If it returns `true`, [initialPassphrase]
   * is returned. Otherwise, if [interactive] is `true`, reads a new passphrase with [readPassphrase] up to
   * [Values.INVALID_PASSPHRASE_TRIES] times, checking each one with [DataHandler.validate] and returning it if `true`,
   * and throws an [IllegalArgumentException] if the passphrase was invalid too many times. If [interactive] is `false`
   * and the initial [DataHandler.validate] returns `false`, an [IllegalStateException] is thrown.
   */
  suspend fun checkValidatorPassphrase(
    dataHandler: DataHandler,
    console: Console,
    interactive: Boolean,
    metadata: SecretMetadata,
    initialPassphrase: ByteString,
    validator: ByteString,
    name: String,
    readPassphrase: suspend () -> ByteString,
  ): ByteString {
    var tries = Values.INVALID_PASSPHRASE_TRIES
    var passphrase = initialPassphrase
    while (tries > 0) {
      tries--
      if (dataHandler.validate(metadata, passphrase, validator)) return passphrase
      if (!interactive) throw IllegalStateException("Invalid $name was given and interactive mode is off")
      console.println("Invalid $name")
      passphrase = readPassphrase()
    }
    if (dataHandler.validate(metadata, passphrase, validator)) return passphrase
    else throw IllegalStateException("Invalid $name entered too many times")
  }
}
