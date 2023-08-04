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

@file:JsModule("react-dom/server.browser")
@file:JsNonModule

package react.dom.server

import react.ReactNode
import web.abort.AbortSignal
import web.streams.ReadableStream
import kotlin.js.Promise

/**
 * TBD.
 */
public external interface ReadableStreamRenderOptions {
  public var identifierPrefix: String?
  public var namespaceURI: String?
  public var nonce: String?
  public var bootstrapScriptContent: String?
  public var bootstrapScripts: List<String>?
  public var bootstrapModules: List<String>?
  public var progressiveChunkSize: Int?
  public var signal: AbortSignal?
  public var onError: (error: dynamic) -> Unit
}

/**
 * TBD.
 */
public external fun renderToReadableStream(
  initialChildren: ReactNode,
  options: ReadableStreamRenderOptions = definedExternally,
): Promise<ReadableStream<ByteArray>>
