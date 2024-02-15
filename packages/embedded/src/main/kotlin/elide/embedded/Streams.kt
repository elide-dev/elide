/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.embedded

import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicReference

/**
 * # Streams
 *
 * Manages static globalized access to standard input, output, and error streams; this interface is used by Logback and
 * Elide Embedded to keep track of the current streams, and swap them for stubs.
 */
public object Streams {
  /**
   * ## System Stream
   *
   * Wraps a stream of type [T] for atomic tracking and access.
   *
   * @param ref Reference to the initial value for this stream.
   */
  public abstract class SystemStream<T> protected constructor(private val ref: AtomicReference<T>) {
    public val stream: T get() = ref.get()

    public companion object {
      @JvmStatic public fun create(stream: InputStream): SystemInputStream = SystemInputStream(stream)
      @JvmStatic public fun create(stream: PrintStream): SystemOutputStream = SystemOutputStream(stream)
    }
  }

  /**
   * ## System Stream: Input
   *
   * Wraps an [InputStream] with [SystemStream].
   */
  public class SystemInputStream internal constructor(stream: InputStream) : SystemStream<InputStream>(
    AtomicReference(stream),
  ), Closeable by stream

  /**
   * ## System Stream: Output
   *
   * Wraps a [PrintStream] with [SystemStream].
   */
  public class SystemOutputStream internal constructor(stream: PrintStream) : SystemStream<PrintStream>(
    AtomicReference(stream),
  ), Closeable by stream

  /**
   * The standard input stream.
   */
  @Suppress("ObjectPropertyNaming")
  @JvmStatic public val `in`: SystemStream<InputStream> = SystemStream.create(System.`in`)

  /**
   * The standard output stream.
   */
  @JvmStatic public val out: SystemStream<PrintStream> = SystemStream.create(System.out)

  /**
   * The standard error stream.
   */
  @JvmStatic public val err: SystemStream<PrintStream> = SystemStream.create(System.err)

  /**
   *
   */
  public interface StreamStub {
    /**
     *
     */
    public val stdin: InputStream

    /**
     *
     */
    public val stdout: PrintStream

    /**
     *
     */
    public val stderr: PrintStream
  }

  /**
   *
   */
  public object StreamStubImpl : StreamStub {
    override val stdin  : InputStream = ByteArrayInputStream(ByteArray(0))
    override val stdout : PrintStream = PrintStream(OutputStream.nullOutputStream())
    override val stderr : PrintStream = PrintStream(OutputStream.nullOutputStream())
  }

  /**
   *
   */
  public val stub: StreamStub = StreamStubImpl
}
