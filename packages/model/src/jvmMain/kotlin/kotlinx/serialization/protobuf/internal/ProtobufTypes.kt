package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor

internal val SerialDescriptor.isPackable: Boolean
  @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
  get() = when (kind) {
    PrimitiveKind.STRING,
    !is PrimitiveKind,
    -> false
    else -> true
  }
