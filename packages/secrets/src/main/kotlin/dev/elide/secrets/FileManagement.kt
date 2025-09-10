package dev.elide.secrets

import dev.elide.secrets.dto.persisted.LocalMetadata
import dev.elide.secrets.dto.persisted.LocalProfile
import dev.elide.secrets.dto.persisted.SecretKey
import dev.elide.secrets.dto.persisted.SecretProfile
import kotlinx.io.bytestring.ByteString

/** @author Lauri Heino <datafox> */
internal interface FileManagement {
  fun metadataExists(): Boolean

  fun readMetadata(): LocalMetadata

  fun writeMetadata(): ByteString

  fun localExists(): Boolean

  fun readLocal(): LocalProfile

  fun writeLocal(): ByteString

  fun canDecryptLocal(passphrase: String): Boolean

  fun profileExists(profile: String): Boolean

  fun readProfile(profile: String): Pair<SecretProfile, SecretKey>

  fun writeProfile(profile: SecretProfile, key: SecretKey): Pair<ByteString, ByteString>

  fun removeProfile(profile: String)

  fun profileBytes(profile: String): ByteString

  fun readKey(profile: String): SecretKey
}
