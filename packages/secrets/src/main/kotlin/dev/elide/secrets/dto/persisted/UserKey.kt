package dev.elide.secrets.dto.persisted

import dev.elide.secrets.impl.ByteStringSerializer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.hexToByteString
import kotlinx.serialization.Serializable

/**
 * Encryption key for secrets.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
internal data class UserKey(
  val mode: EncryptionMode,
  @Serializable(with = ByteStringSerializer::class) val key: ByteString,
) {
  constructor(hashedPassphrase: ByteString) : this(EncryptionMode.PASSPHRASE, hashedPassphrase)

  @OptIn(ExperimentalStdlibApi::class)
  constructor(gpgFingerprint: String) : this(EncryptionMode.GPG, gpgFingerprint.hexToByteString())
}
