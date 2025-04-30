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

@file:Suppress("NOTHING_TO_INLINE")

package elide.tool

/**
 * @return Argument string within the provided argument [context].
 */
public inline fun Argument.asArgumentString(context: ArgumentContext = ArgumentContext.Default): String {
  return context.asArgumentString()
}

/**
 * Create or fill a [StringBuilder] with content rendered from an [Arguments.Suite].
 *
 * @param context Argument context to use for rendering.
 */
public fun Arguments.Suite.toStringBuilder(
  context: ArgumentContext = ArgumentContext.Default,
  builder: StringBuilder = StringBuilder(),
): StringBuilder = builder.also {
  map {
    it.asArgumentString(context)
  }.forEachIndexed { index, value ->
    builder.append(value)
    if (index != size - 1) {
      builder.append(context.argSeparator)
    }
  }
}

/**
 * @return Rendered arguments within the provided argument [context]; if no context is provided, a default is resolved.
 */
public inline fun Arguments.Suite.asArgumentString(context: ArgumentContext = ArgumentContext.Default): String =
  toStringBuilder(context).toString()
