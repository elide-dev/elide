package elide.runtime.js

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array


/** @return The current [ArrayBuffer] as a byte array. */
fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).unsafeCast<ByteArray>()

/** @return The current [ArrayBuffer] as a byte array. */
fun ArrayBuffer?.toByteArray(): ByteArray? = this?.run { Int8Array(this).unsafeCast<ByteArray>() }
