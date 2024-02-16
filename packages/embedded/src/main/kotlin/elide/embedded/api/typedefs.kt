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

package elide.embedded.api

/**
 * ## Exit Code
 *
 * An "exit code" is a non-negative integer, which typically indicates success with `0` values, and failure with any
 * other integer value.
 */
public typealias ExitCode = UInt

/**
 * ## In-flight Call ID
 *
 * Opaque large numbers are used to track in-flight calls; this number is provided back to the native context for use as
 * a call handle.
 */
public typealias InFlightCallID = Long

/** Name of the "baseline" capability. */
public const val BASELINE_NAME: String = "baseline"
