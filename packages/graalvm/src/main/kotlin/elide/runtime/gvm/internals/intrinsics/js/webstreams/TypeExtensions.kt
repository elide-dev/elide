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
package elide.runtime.gvm.internals.intrinsics.js.webstreams

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.ReadableStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamReader

/**
 * Assert that this reader is a [ReadableStreamDefaultReaderImpl], or throw a [TypeError] otherwise. Using contracts,
 * this function allows smart casts to be performed on the receiving value: if it returns without throwing, the type
 * check is guaranteed to succeed.
 *
 * This extension is useful for implementations of stream components, where type casts are often needed.
 */
@OptIn(ExperimentalContracts::class)
internal fun ReadableStreamReader.assertDefault(): ReadableStreamDefaultReaderImpl {
  contract { returns() implies (this@assertDefault is ReadableStreamDefaultReaderImpl) }
  return this as? ReadableStreamDefaultReaderImpl ?: throw TypeError.create("Expected a default reader")
}

/** Use the appropriate method to detach a reader from its stream, depending on the reader type. */
internal fun ReadableStreamReader.detach() = when (this) {
  is ReadableStreamDefaultReaderImpl -> detach()
  else -> throw TypeError.create("Unsupported reader type")
}


/** Use the appropriate method to cancel a stream controller, depending on its type. */
internal fun ReadableStreamController.cancel(reason: Any? = null) = when (this) {
  is ReadableStreamDefaultControllerImpl -> cancel(reason)
  else -> throw TypeError.create("Unsupported controller type")
}
