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

import tools.elide.call.v1alpha1.*
import elide.embedded.api.InFlightCallID
import elide.embedded.api.InFlightCallInfo
import elide.embedded.api.PackedCall
import elide.embedded.api.ProtocolMode.PROTOBUF
import elide.embedded.api.UnaryNativeCall
import elide.embedded.api.UnaryNativeCall.UnaryNativeRequestBuilder
import elide.embedded.impl.InMemoryCallMediator

interface CallBuilder {
  companion object {
    private const val NATIVE_BY_DEFAULT: Boolean = false
  }

  fun createCall(
    callId: InFlightCallID? = null,
    native: Boolean = NATIVE_BY_DEFAULT,
    builder: UnaryNativeRequestBuilder.() -> Unit = {},
  ): Pair<UnaryNativeCall, InFlightCallInfo> {
    val effectiveCallId = callId ?: InMemoryCallMediator.obtain().allocateCallId()
    val info = InFlightCallInfo.of(effectiveCallId)
    val call = if (!native) UnaryNativeCall.buildRequest(effectiveCallId, PROTOBUF, builder) else {
      // build request manually
      val req = UnaryInvocation.newBuilder().apply {
        request = UnaryNativeRequestBuilder().apply(builder).build()
      }.build()

      // encode to bytes
      val buf = PackedCall.pack(req)
      UnaryNativeCall.of(effectiveCallId, PROTOBUF, buf)
    }
    return call to info
  }

  fun createFetch(
    callId: InFlightCallID? = null,
    native: Boolean = NATIVE_BY_DEFAULT,
    builder: FetchRequestKt.Dsl.() -> Unit = {},
  ): Pair<UnaryNativeCall, InFlightCallInfo> {
    return createCall(callId, native = native) {
      request(fetchRequest(builder))
    }
  }

  fun createScheduled(
    callId: InFlightCallID? = null,
    native: Boolean = NATIVE_BY_DEFAULT,
    builder: ScheduledInvocationRequestKt.Dsl.() -> Unit = {},
  ): Pair<UnaryNativeCall, InFlightCallInfo> {
    return createCall(callId, native = native) {
      request(scheduledInvocationRequest(builder))
    }
  }

  fun createQueued(
    callId: InFlightCallID? = null,
    native: Boolean = NATIVE_BY_DEFAULT,
    builder: QueueInvocationRequestKt.Dsl.() -> Unit = {},
  ): Pair<UnaryNativeCall, InFlightCallInfo> {
    return createCall(callId, native = native) {
      request(queueInvocationRequest(builder))
    }
  }
}
