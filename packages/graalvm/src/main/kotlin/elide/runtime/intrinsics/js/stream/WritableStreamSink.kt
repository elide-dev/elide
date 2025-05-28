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
import elide.runtime.intrinsics.js.JsPromise

public interface WritableStreamSink {
  public val type: Any get() = Unit

  public fun start(controller: WritableStreamDefaultController) {}

  public fun write(chunk: Value, controller: WritableStreamDefaultController): JsPromise<Unit> {
    return JsPromise.resolved(Unit)
  }

  public fun close(): JsPromise<Unit> {
    return JsPromise.resolved(Unit)
  }

  public fun abort(reason: Any? = null): JsPromise<Unit> {
    return JsPromise.resolved(Unit)
  }

  /** A no-op sink that simply ignores all writes. */
  public object DiscardingSink : WritableStreamSink
}

@JvmInline public value class GuestWritableStreamSink(public val value: Value) : WritableStreamSink {
  override fun start(controller: WritableStreamDefaultController) {
    if (!value.hasMember(START_MEMBER)) JsPromise.resolved(Unit)
    else value.invokeMember(START_MEMBER, controller)
  }

  override fun write(chunk: Value, controller: WritableStreamDefaultController): JsPromise<Unit> {
    return if (!value.hasMember(WRITE_MEMBER)) JsPromise.resolved(Unit)
    else JsPromise.wrap(value.invokeMember(WRITE_MEMBER, chunk, controller), unwrapFulfilled = { })
  }

  override fun close(): JsPromise<Unit> {
    return if (!value.hasMember(CLOSE_MEMBER)) JsPromise.resolved(Unit)
    else JsPromise.wrap(value.invokeMember(CLOSE_MEMBER), unwrapFulfilled = { })
  }

  override fun abort(reason: Any?): JsPromise<Unit> {
    return if (!value.hasMember(ABORT_MEMBER)) JsPromise.resolved(Unit)
    else JsPromise.wrap(value.invokeMember(ABORT_MEMBER, reason), unwrapFulfilled = { })
  }

  private companion object {
    private const val START_MEMBER = "start"
    private const val WRITE_MEMBER = "write"
    private const val CLOSE_MEMBER = "close"
    private const val ABORT_MEMBER = "abort"
  }
}
