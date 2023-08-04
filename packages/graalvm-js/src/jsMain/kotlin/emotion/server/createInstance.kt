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

@file:JsModule("@emotion/server/create-instance")
@file:OptIn(ExperimentalJsExport::class)

package emotion.server

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
public external fun createEmotionServer(cache: dynamic): EmotionServer
