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

import kotlinx.datetime.Clock
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.js.node.BufferAPI
import elide.vm.annotations.Polyglot

@DelicateElideApi internal class NodeFile private constructor(
  bytes: ByteArray,
  type: String?,
  @Polyglot override val file: String,
  @Polyglot override val lastModified: Long,
) : NodeBlob(bytes, type), BufferAPI.File {
  @Polyglot constructor(sources: PolyglotValue, fileName: PolyglotValue) : this(
    sources = sources,
    fileName = fileName,
    options = null,
  )

  @Polyglot constructor(sources: PolyglotValue, fileName: PolyglotValue, options: PolyglotValue?) : this(
    bytes = makeBlobBytes(sources, options),
    type = NewBlobOptions.type(options),
    file = fileName.asString(),
    lastModified = NewFileOptions.lastModified(options),
  )

  private object NewFileOptions {
    fun lastModified(options: PolyglotValue?): Long {
      return options?.getMember("lastModified")?.asLong() ?: Clock.System.now().toEpochMilliseconds()
    }
  }
}
