@file:JsModule("emotion-server")
@file:JsNonModule
package emotion.server.worker

import emotion.cache.EmotionCache

/**
 * TBD.
 */
external interface StyleInfo {
  /** TBD. */
  var key: String

  /** TBD. */
  var ids: List<String>

  /** TBD. */
  var css: String
}

/**
 * TBD.
 */
external interface EmotionCriticalToChunks {
  /** TBD. */
  var html: String

  /** TBD. */
  var styles: List<StyleInfo>
}

/**
 * TBD.
 */
external interface EmotionServer {
  /** TBD. */
  fun constructStyleTagsFromChunks(critical: EmotionCriticalToChunks): String

  /** TBD. */
  fun extractCriticalToChunks(html: String): EmotionCriticalToChunks
}

/**
 * TBD.
 */
external fun createEmotionServer(cache: EmotionCache): EmotionServer
