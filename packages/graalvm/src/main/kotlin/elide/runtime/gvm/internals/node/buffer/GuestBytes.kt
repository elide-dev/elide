package elide.runtime.gvm.internals.node.buffer

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/**
 * A light wrapper around a guest buffer-like value, providing indexed access. This helper can be used to wrap the
 * `ArrayBuffer` instances backing a `Uint8Array` or similar, for convenience when manipulating them.
 *
 * Note that no validation of any kind is performed during construction or by any of the operators.
 */
@DelicateElideApi @JvmInline internal value class GuestBytes(val value: PolyglotValue) {
  /** Returns the length in bytes of the wrapped value. */
  val size: Int get() = value.bufferSize.toInt()

  /** Returns the length in bytes of the wrapped value as a [Long], useful for certain operations. */
  val longSize: Long get() = value.bufferSize

  /** Returns the value of the wrapped buffer at the given [index]. */
  operator fun get(index: Long): Byte {
    return value.readBufferByte(index)
  }

  /** Returns the value of the wrapped buffer at the given [index]. */
  operator fun get(index: Int): Byte {
    return value.readBufferByte(index.toLong())
  }

  /** Sets the [value] of the wrapped buffer at the given [index]. */
  operator fun set(index: Long, value: Byte) {
    this.value.writeBufferByte(index, value)
  }

  /** Sets the [value] of the wrapped buffer at the given [index]. */
  operator fun set(index: Int, value: Byte) {
    this.value.writeBufferByte(index.toLong(), value)
  }
}
