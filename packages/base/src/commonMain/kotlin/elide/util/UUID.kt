package elide.util

/** UUID tools provided to all platforms. */
public expect object UUID {
  /**
   * Generate a random UUIDv4, based on no input data at all.
   *
   * @return Randomly-generated UUID.
   */
  public fun random(): String
}
