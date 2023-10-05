/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package kotlinx.serialization.protobuf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * # Proto: Unknown Value
 *
 * Marks a value on a protocol-buffers enumeration as the "unknown," or default value, for the enumeration. If no value
 * is specified over the wire, this value will be returned. The ID for this value in the enumeration is always `0`.
 */
@SerialInfo
@MustBeDocumented
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoUnknown
