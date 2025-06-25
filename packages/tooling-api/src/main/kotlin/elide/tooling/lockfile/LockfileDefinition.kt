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
package elide.tooling.lockfile

import java.io.InputStream
import java.io.OutputStream

/**
 * # Lockfile Definition
 *
 * Specifies the expected layout of the external wrapping class that defines a lockfile version.
 */
public sealed interface LockfileDefinition<T> where T: ElideLockfile {
  /**
   * ## Read from Stream
   *
   * Reads a lockfile from the given [stream], using the provided [format].
   *
   * @param format The format of the lockfile to read.
   * @param stream The input stream to read from.
   * @return The lockfile read from the stream.
   */
  public fun readFrom(format: ElideLockfile.Format, stream: InputStream): T

  /**
   * ## Write to Stream
   *
   * Writes the lockfile to the given [stream], using the provided [format].
   *
   * @param format The format of the lockfile to write.
   * @param lockfile The lockfile to write.
   * @param stream The output stream to write to.
   * @return The lockfile written to the stream.
   */
  public fun writeTo(format: ElideLockfile.Format, lockfile: ElideLockfile, stream: OutputStream)
}
