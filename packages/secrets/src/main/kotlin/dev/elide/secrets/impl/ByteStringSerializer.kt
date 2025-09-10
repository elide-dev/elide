package dev.elide.secrets.impl

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal class ByteStringSerializer : KSerializer<ByteString> {
  override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

  override fun serialize(encoder: Encoder, value: ByteString) {
    ByteArraySerializer().serialize(encoder, value.toByteArray())
  }

  override fun deserialize(decoder: Decoder): ByteString {
    return ByteString(ByteArraySerializer().deserialize(decoder))
  }
}
