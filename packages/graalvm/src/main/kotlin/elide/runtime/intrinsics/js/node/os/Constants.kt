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
package elide.runtime.intrinsics.js.node.os

/**
 * Endianness symbolic type.
 */
public typealias Endianness = String

/**
 * Symbol used to indicate little-endianness.
 */
public const val ENDIAN_LITTLE: Endianness = "LE"

/**
 * Symbol used to indicate big-endianness.
 */
public const val ENDIAN_BIG: Endianness = "BE"

/**
 * Priority setting for a given OS task.
 */
public typealias Priority = Int

/**
 * Execution Priority: `PRIORITY_HIGHEST`.
 *
 * Sets the highest possible execution priority for a given OS task; on some platforms (e.g. Windows), this may require
 * elevated privileges.
 */
public const val PRIORITY_HIGHEST: Priority = -20

/**
 * Execution Priority: `PRIORITY_HIGH`.
 *
 * Sets a high execution priority for a given OS task.
 */
public const val PRIORITY_HIGH: Priority = -10

/**
 * Execution Priority: `PRIORITY_ABOVE_NORMAL`.
 *
 * Sets an above-normal execution priority for a given OS task.
 */
public const val PRIORITY_ABOVE_NORMAL: Priority = -1

/**
 * Execution Priority: `PRIORITY_NORMAL`.
 *
 * Sets a normal execution priority for a given OS task.
 */
public const val PRIORITY_NORMAL: Priority = 0

/**
 * Execution Priority: `PRIORITY_BELOW_NORMAL`.
 *
 * Sets a below-normal execution priority for a given OS task.
 */
public const val PRIORITY_BELOW_NORMAL: Priority = 1

/**
 * Execution Priority: `PRIORITY_LOW`.
 *
 * Sets a low execution priority for a given OS task.
 */
public const val PRIORITY_LOW: Priority = 19
