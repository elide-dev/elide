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
import elide.runtime.intrinsics.js.ReadableStream.Type

public interface TransformStreamTransformer {
  public val readableType: Type get() = Type.Default
  public val writableType: Any get() = Unit

  public fun start(controller: TransformStreamDefaultController): JsPromise<Unit> = JsPromise.resolved(Unit)
  public fun flush(controller: TransformStreamDefaultController): JsPromise<Unit> = JsPromise.resolved(Unit)

  public fun transform(chunk: Value, controller: TransformStreamDefaultController): JsPromise<Unit> {
    controller.enqueue(chunk)
    return JsPromise.resolved(Unit)
  }

  public fun cancel(reason: Any? = null): JsPromise<Unit> = JsPromise.resolved(Unit)

  public object Identity : TransformStreamTransformer {
    override fun transform(chunk: Value, controller: TransformStreamDefaultController): JsPromise<Unit> {
      controller.enqueue(chunk)
      return JsPromise.resolved(Unit)
    }
  }
}

@JvmInline public value class GuestTransformStreamTransformer(public val value: Value) : TransformStreamTransformer {
  override val readableType: Type
    get() {
      return if (value.hasMember(READABLE_TYPE_MEMBER)) {
        Type.fromGuestValue(value.getMember(READABLE_TYPE_MEMBER).asString())
      } else {
        super.readableType
      }
    }

  override fun start(controller: TransformStreamDefaultController): JsPromise<Unit> {
    if (!value.hasMember(START_MEMBER)) return super.start(controller)
    return JsPromise.wrap(value.invokeMember(START_MEMBER, controller), unwrapFulfilled = { })
  }

  override fun transform(chunk: Value, controller: TransformStreamDefaultController): JsPromise<Unit> {
    if (!value.hasMember(TRANSFORM_MEMBER)) return JsPromise.resolved(Unit)
    return JsPromise.wrap(value.invokeMember(TRANSFORM_MEMBER, chunk, controller), unwrapFulfilled = { })
  }

  override fun flush(controller: TransformStreamDefaultController): JsPromise<Unit> {
    if (!value.hasMember(FLUSH_MEMBER)) return JsPromise.resolved(Unit)
    return JsPromise.wrap(value.invokeMember(FLUSH_MEMBER, controller), unwrapFulfilled = { })
  }

  override fun cancel(reason: Any?): JsPromise<Unit> {
    if (!value.hasMember(CANCEL_MEMBER)) return JsPromise.resolved(Unit)
    return JsPromise.wrap(value.invokeMember(CANCEL_MEMBER, reason), unwrapFulfilled = { })
  }

  private companion object {
    private const val READABLE_TYPE_MEMBER = "readableType"
    private const val START_MEMBER = "start"
    private const val TRANSFORM_MEMBER = "transform"
    private const val CANCEL_MEMBER = "cancel"
    private const val FLUSH_MEMBER = "flush"
  }
}
