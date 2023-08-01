@file:JsModule("emotion-server")
@file:JsNonModule

package emotion.server.worker

import emotion.cache.EmotionCache

/**
 * Describes a package of style info included with a [EmotionCriticalToChunks] payload.
 */
external interface StyleInfo {
  /** Unique key. */
  var key: String

  /** CSS IDs held by this chunk. */
  var ids: List<String>

  /** CSS code held by this chunk. */
  var css: String
}

/**
 * Holds rendered HTML and style info, with critical CSS broken out.
 */
external interface EmotionCriticalToChunks {
  /** HTML code. */
  var html: String

  /** List of style info chunks. */
  var styles: List<StyleInfo>
}

/**
 * Represents an Emotion CSS server-side rendering worker.
 */
external interface EmotionServer {
  /** @return CSS style tags from provided [critical] chunks. */
  fun constructStyleTagsFromChunks(critical: EmotionCriticalToChunks): String

  /** @return Critical chunks for provided [html] code. */
  fun extractCriticalToChunks(html: String): EmotionCriticalToChunks
}

/**
 * Create an emotion server which uses the provided [cache].
 *
 * The cache, ideally, should be statically held in a module variable to persist across page-loads.
 *
 * @param cache Emotion cache to use.
 * @return Emotion server.
 */
external fun createEmotionServer(cache: EmotionCache): EmotionServer
