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

import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.GuestJsPromise
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.ReadableStream.Type

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

  /** A placeholder, no-op source that can be used as a default value. */
  public object Empty : ReadableStreamSource
}

/**
 * A wrapper around a guest [value] that allows its use as a [ReadableStreamSource]. All methods delegate to invoking
 * the corresponding member; if the member is not present, the method does nothing.
 */
@JvmInline public value class GuestReadableStreamSource(public val value: Value) : ReadableStreamSource {
  override val autoAllocateChunkSize: Long
    get() = if (value.hasMember(ALLOCATE_SIZE_MEMBER)) value.getMember(ALLOCATE_SIZE_MEMBER).asLong()
    else super.autoAllocateChunkSize

  override val type: Type
    get() = if (value.hasMember(TYPE_MEMBER)) Type.fromGuestValue(value.getMember(TYPE_MEMBER).asString())
    else super.type

  override fun start(controller: ReadableStreamController) {
    if (!value.hasMember(START_MEMBER)) return
    value.invokeMember(START_MEMBER, controller)
  }

  override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
    if (!value.hasMember(PULL_MEMBER)) JsPromise.resolved(Unit)
    return GuestJsPromise.from(value.invokeMember(PULL_MEMBER, controller))
  }

  override fun cancel(reason: Any?): JsPromise<Unit> {
    if (!value.hasMember(CANCEL_MEMBER)) return JsPromise.resolved(Unit)
    return GuestJsPromise.from(value.invokeMember(CANCEL_MEMBER, reason))
  }

  private companion object {
    private const val ALLOCATE_SIZE_MEMBER = "autoAllocateChunkSize"
    private const val TYPE_MEMBER = "type"
    private const val START_MEMBER = "start"
    private const val PULL_MEMBER = "pull"
    private const val CANCEL_MEMBER = "cancel"
  }
}
