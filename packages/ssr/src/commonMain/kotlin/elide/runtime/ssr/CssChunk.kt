package elide.runtime.ssr

import elide.annotations.core.Polyglot

/**
 * TBD
 */
public data class CssChunk @Polyglot constructor (
  @field:Polyglot public val ids: Array<String>,
  @field:Polyglot public val key: String,
  @field:Polyglot public val css: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as CssChunk

    if (key != other.key) return false

    return true
  }

  override fun hashCode(): Int {
    return key.hashCode()
  }
}
