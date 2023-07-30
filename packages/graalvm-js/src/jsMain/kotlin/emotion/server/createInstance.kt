@file:JsModule("@emotion/server/create-instance")
@file:OptIn(ExperimentalJsExport::class)

package emotion.server

import emotion.cache.EmotionCache

/**
 *
 */
@JsExport
public external interface EmotionCritical {
  public var html: String
  public var ids: Array<String>
  public var css: String
}

/**
 *
 */
@JsExport
public external interface EmotionStyleChunk {
  public var key: String
  public var ids: Array<String>
  public var css: String
}

/**
 *
 */
@JsExport
public external interface EmotionCriticalToChunks {
  public var html: String
  public var styles: Array<EmotionStyleChunk>
}

/**
 *
 */
@JsExport
public external interface EmotionServer {
  /**
   *
   */
  public fun extractCritical(html: String): EmotionCritical

  /**
   *
   */
  public fun extractCriticalToChunks(html: String): EmotionCriticalToChunks

  /**
   *
   */
  public fun renderStylesToString(html: String): String

  /**
   *
   */
  public fun constructStyleTagsFromChunks(criticalData: EmotionCriticalToChunks): String
}

/**
 *
 */
@JsName("default")
public external fun createEmotionServer(cache: EmotionCache): EmotionServer
