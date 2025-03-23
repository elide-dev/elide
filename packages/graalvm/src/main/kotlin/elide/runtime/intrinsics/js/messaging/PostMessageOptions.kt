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
package elide.runtime.intrinsics.js.messaging

import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.Transferable

/**
 * ## Post Message Options
 *
 * Describes options that can apply to a `postMessage` operation as part of a `MessagePort`. The only available option,
 * [transfer], specifies objects that should be transferred to the receiving context instead of cloned.
 *
 * @property transfer Objects that should be transferred to the receiving context instead of cloned.
 */
@JvmRecord public data class PostMessageOptions(
  public val transfer: Collection<Transferable> = emptyList(),
) {
  /** Factory utilities for [PostMessageOptions]. */
  public companion object {
    // After selecting a guest value to build transferable values from, use it to build them into a list.
    @JvmStatic private fun buildTransferrablesFrom(value: Value): Collection<Transferable> {
      return buildList(value.arraySize.toInt()) {
        for (i in 0 until value.arraySize) {
          add(value.getArrayElement(i).`as`<Transferable>(Transferable::class.java))
        }
      }
    }

    // Build a set of transferable objects from the provided guest value.
    @JvmStatic private fun buildTransferrables(value: Value?): Collection<Transferable> {
      return when {
        // nulls -> empty list
        value == null || value.isNull -> emptyList()

        // has array members directly -> build from value
        value.hasArrayElements() -> buildTransferrablesFrom(value)

        // object with `transfer` array -> build from that
        value.hasMembers() -> if (value.hasMember("transfer")) {
          buildTransferrablesFrom(value.getMember("transfer"))
        } else {
          emptyList()
        }

        // otherwise, empty list
        else -> emptyList()
      }
    }

    /** @return Empty/default [PostMessageOptions]. */
    @JvmStatic public fun empty(): PostMessageOptions = PostMessageOptions()

    /** @return [PostMessageOptions] with the provided [transfer] objects. */
    @JvmStatic public fun of(vararg transfer: Transferable): PostMessageOptions = PostMessageOptions(transfer.toList())

    /** @return [PostMessageOptions] with the provided [transfer] objects. */
    @JvmStatic public fun of(transfer: Collection<Transferable>): PostMessageOptions = PostMessageOptions(transfer)

    /** @return [PostMessageOptions] from the provided [value]. */
    @JvmStatic public fun from(value: Value?): PostMessageOptions = PostMessageOptions(
      transfer = buildTransferrables(value),
    )
  }
}
