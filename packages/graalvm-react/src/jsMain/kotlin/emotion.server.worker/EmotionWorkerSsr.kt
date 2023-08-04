/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress("unused")
@file:JsModule("emotion-server")
@file:JsNonModule
package emotion.server.worker

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
external fun createEmotionServer(cache: dynamic): EmotionServer
