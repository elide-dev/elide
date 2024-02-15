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

@file:OptIn(ExperimentalSerializationApi::class)
@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package elide.http.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.jvm.JvmStatic
import elide.annotations.API

/**
 * # HTTP: Standard Method
 *
 * Enumerates standard HTTP request methods, binding them to unique identifiers for serialization, and providing various
 * utility methods for working with HTTP methods.
 *
 * @see HttpMethod for the interface guaranteed by HTTP method description classes or objects.
 * @param write Whether this operation is expected to modify the state of the server or its resources; if `true`, the
 *   operation is considered a write operation, and is not expected to be idempotent by default.
 * @param idempotent Whether this operation is expected to perform idempotent work; if `true`, the operation is
 *   considered safe for certain use cases.
 */
@API @Serializable public enum class StandardHttpMethod (
  override val write: Boolean = false,
  override val body: Boolean = false,
  override val idempotent: Boolean = !write,
) : HttpMethod {
  /** HTTP GET method. */
  @ProtoNumber(1) GET,

  /** HTTP POST method. */
  @ProtoNumber(2) POST(write = true, body = true),

  /** HTTP PUT method. */
  @ProtoNumber(3) PUT(write = true, body = true),

  /** HTTP DELETE method. */
  @ProtoNumber(4) DELETE(write = true),

  /** HTTP PATCH method. */
  @ProtoNumber(5) PATCH(write = true, body = true),

  /** HTTP HEAD method. */
  @ProtoNumber(6) HEAD,

  /** HTTP OPTIONS method. */
  @ProtoNumber(7) OPTIONS,

  /** HTTP TRACE method. */
  @ProtoNumber(8) TRACE(idempotent = false),

  /** HTTP CONNECT method. */
  @ProtoNumber(9) CONNECT(idempotent = false);

  /** Utilities for dealing with HTTP methods. */
  public companion object {
    /** HTTP methods which might expect to write. */
    @JvmStatic public val writeMethods: Set<StandardHttpMethod> = setOf(POST, PUT, DELETE, PATCH)

    /** HTTP methods which generally do not expect to write. */
    @JvmStatic public val readMethods: Set<StandardHttpMethod> = setOf(GET, HEAD, OPTIONS)
  }
}
