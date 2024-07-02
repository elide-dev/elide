package elide.runtime.gvm.internals.node.buffer

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/**
 * A wrapper around a `TypedArray` (e.g. `Uint8Array`), `Buffer`, or any view-like value which exposes an inner
 * `ArrayBuffer`, offset, and length.
 *
 * This wrapper can be used to easily interact with typed arrays and their inner array buffers. Use [tryFrom] to
 * attempt wrapping a value without throwing an exception for invalid cases.
 */
@DelicateElideApi @JvmInline internal value class GuestBufferView private constructor(val value: PolyglotValue) {
  /**
   * Returns the array buffer backing this view, as a [GuestBytes] wrapper. The returned value is validated and is
   * guaranteed to contain buffer elements.
   */
  fun bytes(): GuestBytes {
    val bytes = value.getMember(MEMBER_BUFFER)
    check(bytes.hasBufferElements()) { "Expected the view's backing buffer to have buffer elements" }
    return GuestBytes(bytes)
  }

  /** Returns the offset in the backing array buffer at which this view starts. */
  fun byteOffset(): Int {
    return value.getMember(MEMBER_OFFSET).asInt()
  }

  /** Returns the size in bytes of this view. */
  fun byteSize(): Int {
    return value.getMember(MEMBER_LENGTH).asInt()
  }

  companion object {
    /** Member name for the backing buffer value. */
    private const val MEMBER_BUFFER = "buffer"

    /** Member name for the view's byte offset. */
    private const val MEMBER_OFFSET = "byteOffset"

    /** Member name for the view's byte length. */
    private const val MEMBER_LENGTH = "byteLength"

    /**
     * If the given [value] is a buffer view (e.g. `TypedArray`, `Buffer`), returns a [GuestBufferView] wrapping it,
     * otherwise returns `null`.
     */
    fun tryFrom(value: PolyglotValue): GuestBufferView? {
      return if (!value.hasMembers() || !value.hasMember(MEMBER_BUFFER) || !value.hasMember(MEMBER_OFFSET)) null
      else GuestBufferView(value)
    }
  }
}