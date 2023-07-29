package elide.ssr

import elide.vm.annotations.Polyglot

/**
 * # SSR: CSS Chunk
 *
 * Data class which holds a chunk of CSS code, which should be emitted as part of a larger SSR run. This class is used
 * in particular for integration with Emotion's SSR engine.
 *
 * @param ids CSS IDs held by this chunk.
 * @param key Unique key identifying this chunk.
 * @param css CSS code held by this chunk.
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

  override fun toString(): String {
    return "CssChunk(key=$key)"
  }
}
