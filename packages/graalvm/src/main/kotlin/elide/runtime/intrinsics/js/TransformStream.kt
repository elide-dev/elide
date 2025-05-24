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
import elide.runtime.exec.GuestExecution
import elide.runtime.gvm.internals.intrinsics.js.webstreams.TransformDefaultStream
import elide.runtime.intrinsics.js.stream.GuestQueueingStrategy
import elide.runtime.intrinsics.js.stream.GuestTransformStreamTransformer
import elide.runtime.intrinsics.js.stream.QueueingStrategy
import elide.runtime.intrinsics.js.stream.TransformStreamTransformer
import elide.vm.annotations.Polyglot

/**
 * # Transform Stream
 *
 * Specifies the interface provided by, and expected from, transforming or mutating [Stream] implementations, which
 * comply with the Web Streams standard.
 */
@API public interface TransformStream : Stream {
  /**
   * ## Transform Stream: Factory.
   *
   * Describes constructors available both in guest and host contexts, which create [TransformStream] implementation
   * instances. Generally, each implementation has a factory implementation as well, from which instances can be
   * acquired by host-side code, and, where supported, by guest-side code.
   *
   * @param Impl Implementation of [TransformStream] which is created by this factory.
   */
  public interface Factory<Impl> where Impl : TransformStream

  @get:Polyglot public val readable: ReadableStream
  @get:Polyglot public val writable: WritableStream

  public companion object : Factory<TransformStream>, ProxyInstantiable {
    private val streamExecutor = GuestExecution.workStealing()

    override fun newInstance(vararg arguments: Value?): Any {
      val transformer = arguments.getOrNull(0)?.let(::GuestTransformStreamTransformer)
      val writableStrategy = arguments.getOrNull(1)?.let(GuestQueueingStrategy::from)
      val readableStrategy = arguments.getOrNull(2)?.let(GuestQueueingStrategy::from)

      return TransformDefaultStream(
        transformer = transformer ?: TransformStreamTransformer.Identity,
        executor = streamExecutor,
        writableStrategy = writableStrategy ?: QueueingStrategy.DefaultWriteStrategy,
        readableStrategy = readableStrategy ?: QueueingStrategy.DefaultReadStrategy,
      )
    }
  }
}
