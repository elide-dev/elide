package elide.util

import dev.elide.uuid.uuid4

/** UUID tools provided to all platforms. */
public actual object UUID {
  /**
   * Generate a random UUIDv4, based on no input data at all.
   *
   * @return Randomly-generated UUID.
   */
  public actual fun random(): String {
    return uuid4().toString().uppercase()
  }
}
