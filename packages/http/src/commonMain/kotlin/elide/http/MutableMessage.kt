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

package elide.http

/**
 * ## HTTP Message (Mutable)
 *
 * Represents an abstract HTTP message (a response or request) that is mutable. Mutable requests/responses typically do
 * not provide thread safety guarantees, but can be changed in-place.
 */
public sealed interface MutableMessage: Message {
  /**
   * ### HTTP headers.
   *
   * Mutable HTTP headers; otherwise, behaves the same as regular HTTP headers.
   */
  override val headers: MutableHeaders

  /**
   * Build this mutable message into an immutable form; if the message is already immutable, this is a no-op.
   *
   * @return An immutable message object.
   */
  public fun build(): Message

  /**
   * Platform extension point for mutable HTTP message common logic.
   */
  public interface PlatformMutableMessage: Message
}
