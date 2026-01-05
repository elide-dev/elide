package elide.manager

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import elide.runtime.core.HostPlatform

/** Serializer for [elide.runtime.core.HostPlatform]. */
internal class HostPlatformSerializer : KSerializer<HostPlatform> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("elide.runtime.core.HostPlatform", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: HostPlatform) = encoder.encodeString(value.platformString())

  override fun deserialize(decoder: Decoder): HostPlatform = HostPlatform.parsePlatform(decoder.decodeString())
}
