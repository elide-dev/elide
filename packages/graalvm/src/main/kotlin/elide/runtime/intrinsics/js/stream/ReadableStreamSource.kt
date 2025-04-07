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
package elide.runtime.intrinsics.js.stream

import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream

/**
 * Represents an abstract source of data for a [ReadableStream] as specified by the
 * [WHATWG standard](https://streams.spec.whatwg.org/#underlying-source-api).
 *
 * Sources may be provided by host or guest code, and may or may not support backpressure control.
 */
public interface ReadableStreamSource {
  /** The type of stream this source is compatible with. */
  public val type: ReadableStream.Type get() = ReadableStream.Type.Default

  /** Optional size of the buffers allocated for byte sources. Set to a positive value to enable. */
  public val autoAllocateChunkSize: Long get() = -1L

  /** Called to initialize the source when the [controller] is created. Use this callback to set up the source. */
  public fun start(controller: ReadableStreamController): Unit = Unit

  /** Called by the controller when new chunks are needed. */
  public fun pull(controller: ReadableStreamController): JsPromise<Unit> = JsPromise.Companion.resolved(Unit)

  /** Called when the stream is cancelled by a consumer to release the source. */
  public fun cancel(reason: Any? = null): JsPromise<Unit> = JsPromise.Companion.resolved(Unit)
}
