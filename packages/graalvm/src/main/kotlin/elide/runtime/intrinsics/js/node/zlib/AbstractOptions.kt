/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.intrinsics.js.node.zlib

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.js.JsError

// Implements basic `ProxyObject` functionality for `ZlibOptions` and friends.
internal interface ZlibOptionsDefaults : ProxyObject, ZlibOptions {
  override fun getMemberKeys(): Array<String> = arrayOf(
    "flush",
    "finishFlush",
    "chunkSize",
    "windowBits",
    "level",
    "memLevel",
    "strategy",
    "dictionary",
    "info",
    "maxOutputLength",
  )

  override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
  override fun putMember(key: String?, value: Value?): Unit = JsError.error("Cannot modify ZlibOptions")

  override fun getMember(key: String?): Any? = when (key) {
    "flush" -> flush
    "finishFlush" -> finishFlush
    "chunkSize" -> chunkSize
    "windowBits" -> windowBits
    "level" -> level
    "memLevel" -> memLevel
    "strategy" -> strategy
    "dictionary" -> dictionary
    "info" -> info
    "maxOutputLength" -> maxOutputLength
    else -> null
  }
}

// Implements basic `ProxyObject` functionality for `BrotliOptions`.
internal interface BrotliOptionsDefaults : ProxyObject, BrotliOptions {
  override fun getMemberKeys(): Array<String> = arrayOf(
    "flush",
    "finishFlush",
    "chunkSize",
    "params",
    "maxOutputLength",
  )

  override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
  override fun putMember(key: String?, value: Value?): Unit = JsError.error("Cannot modify BrotliOptions")

  override fun getMember(key: String?): Any? = when (key) {
    "flush" -> flush
    "finishFlush" -> finishFlush
    "chunkSize" -> chunkSize
    "params" -> params
    "maxOutputLength" -> maxOutputLength
    else -> null
  }
}
