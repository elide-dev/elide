package elide.util

import lib.uuid.v4 as uuidv4

/** UUID tools provided to all platforms. */
actual object UUID {
  /**
   * Generate a random UUIDv4, based on no input data at all.
   *
   * @return Randomly-generated UUID.
   */
  actual fun random(): String {
    return uuidv4(null).uppercase()
  }
}
