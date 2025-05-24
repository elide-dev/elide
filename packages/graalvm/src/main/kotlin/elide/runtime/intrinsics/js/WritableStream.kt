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
package elide.runtime.intrinsics.js

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import elide.annotations.API
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableDefaultStream
import elide.runtime.intrinsics.js.stream.*
import elide.vm.annotations.Polyglot

/**
 * # Writable Stream
 *
 * Specifies the interface provided by, and expected from, writable [Stream] implementations, which comply with the Web
 * Streams standard.
 */
@API public interface WritableStream : Stream {
  /**
   * ## Writable Stream: Factory.
   *
   * Describes constructors available both in guest and host contexts, which create [WritableStream] implementation
   * instances. Generally, each implementation has a factory implementation as well, from which instances can be
   * acquired by host-side code, and, where supported, by guest-side code.
   *
   * @param Impl Implementation of [WritableStream] which is created by this factory.
   */
  public interface Factory<Impl> where Impl : WritableStream

  @get:Polyglot public val locked: Boolean

  @Polyglot public fun getWriter(): WritableStreamDefaultWriter
  @Polyglot public fun abort(reason: Any? = null): JsPromise<Unit>
  @Polyglot public fun close(): JsPromise<Unit>

  public companion object : Factory<WritableStream>, ProxyInstantiable {
    override fun newInstance(vararg arguments: Value?): Any {
      val sink = arguments.getOrNull(0)?.let(::GuestWritableStreamSink)
      val strategy = arguments.getOrNull(1)?.let(GuestQueueingStrategy::from)

      return WritableDefaultStream(sink ?: WritableStreamSink.Empty, strategy ?: QueueingStrategy.DefaultWriteStrategy)
    }
  }
}
