/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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
import elide.annotations.API

/**
 *
 */
@API @Serializable public enum class StandardHttpMethod : HttpMethod {
  /** HTTP GET method. */
  @ProtoNumber(1) GET,

  /** HTTP POST method. */
  @ProtoNumber(2) POST,

  /** HTTP PUT method. */
  @ProtoNumber(3) PUT,

  /** HTTP DELETE method. */
  @ProtoNumber(4) DELETE,

  /** HTTP PATCH method. */
  @ProtoNumber(5) PATCH,

  /** HTTP HEAD method. */
  @ProtoNumber(6) HEAD,

  /** HTTP OPTIONS method. */
  @ProtoNumber(7) OPTIONS,

  /** HTTP TRACE method. */
  @ProtoNumber(8) TRACE,

  /** HTTP CONNECT method. */
  @ProtoNumber(9) CONNECT,
}
