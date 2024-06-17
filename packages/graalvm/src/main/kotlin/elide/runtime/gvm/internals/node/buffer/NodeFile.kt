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

package elide.runtime.gvm.internals.node.buffer

import org.graalvm.polyglot.proxy.ProxyObject
import kotlinx.datetime.Clock
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.js.node.BufferAPI
import elide.vm.annotations.Polyglot

/**
 * Implements the `File` class from the Node.js `buffer` built-in module. This class is basically a specialized
 * [NodeBlob] with [name] and [lastModified] fields.
 */
@DelicateElideApi internal class NodeFile private constructor(
  bytes: ByteArray,
  type: String?,
  @Polyglot override val name: String,
  @Polyglot override val lastModified: Long,
) : NodeBlob(bytes, type), BufferAPI.File, ProxyObject {
  /**
   * Create a new [NodeFile] with the given [sources] and [fileName], using default options. The [sources] value must
   * have array elements of the supported type, meaning each item should either have buffer elements, or expose a
   * `buffer` property that does. The [fileName] must be a string value.
   */
  @Polyglot constructor(sources: PolyglotValue, fileName: PolyglotValue) : this(
    sources = sources,
    fileName = fileName,
    options = null,
  )

  /**
   * Create a new [NodeFile] with the given [sources], [fileName], and options. The [sources] value must have array
   * elements of the supported type, meaning each item should either have buffer elements, or expose a `buffer`
   * property that does. The [fileName] must be a string value.
   *
   * The [options] object may specify a `type` property with a string value to set the [type] field, and a
   * `lastModified` property with a numeric value to set the [lastModified] field.
   */
  @Polyglot constructor(sources: PolyglotValue, fileName: PolyglotValue, options: PolyglotValue?) : this(
    bytes = makeBlobBytes(sources, options),
    type = NewBlobOptions.type(options),
    name = fileName.asString(),
    lastModified = NewFileOptions.lastModified(options),
  )

  override fun getMemberKeys(): Any {
    return members
  }

  override fun hasMember(key: String): Boolean {
    return members.binarySearch(key) >= 0
  }

  override fun getMember(key: String?): Any? = when (key) {
    "name" -> name
    "lastModified" -> size
    else -> super.getMember(key)
  }

  override fun putMember(key: String?, value: PolyglotValue?) {
    throw UnsupportedOperationException("Cannot modify 'File' object")
  }

  /** Helper object used to extract fields from constructor options. */
  private object NewFileOptions {
    /**
     * Read the value of the `lastModified` property in an options object passed to the constructor, if present. If no
     * such property exists, the current epoch timestamp is returned instead.
     */
    fun lastModified(options: PolyglotValue?): Long {
      return options?.getMember("lastModified")?.asLong() ?: Clock.System.now().toEpochMilliseconds()
    }
  }

  private companion object {
    /** Static, sorted array of members visible to guest code, including those inherited from [NodeBlob]. */
    private val members = arrayOf(
      "arrayBuffer",
      "size",
      "slice",
      "stream",
      "text",
      "type",
      "name",
      "lastModified",
    ).apply { sort() }
  }
}
