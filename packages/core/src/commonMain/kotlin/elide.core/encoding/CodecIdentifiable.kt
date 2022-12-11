package elide.core.encoding

/**
 * # Codec Identifiable
 *
 * Describes a component of a [Codec], or a generic data object that can identify an [Encoding]. Raw data outputs are
 * also [CodecIdentifiable] by default.
 */
public interface CodecIdentifiable {
  /**
   * Return the enumerated [Encoding] which is implemented by this [Codec].
   *
   * @return Enumerated encoding type.
   */
  public fun encoding(): Encoding
}
