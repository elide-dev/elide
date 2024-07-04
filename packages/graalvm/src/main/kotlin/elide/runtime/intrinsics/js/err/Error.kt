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
package elide.runtime.intrinsics.js.err

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.GuestError
import elide.vm.annotations.Polyglot

// Base properties and methods exposed for guest code, from a JavaScript `Error` instance.
private val JS_ERROR_PROPS_AND_METHODS = arrayOf(
  "message",
  "name",
  "errno",
  "cause",
  "fileName",
  "lineNumber",
  "columnNumber",
  "stackTrace",
)

/**
 * # JavaScript Error
 *
 * Describes the generic interface defined by JavaScript guest errors; properties on instances of this class are made
 * available to guest code.
 */
public abstract class Error public constructor(extraProps: Array<Pair<String, Value>>) :
  AbstractJsException, GuestError, RuntimeException(), ProxyObject {
  // Extra properties which should be exposed from `Error` types.
  private val extraProps: Map<String, Value> = if (extraProps.isNotEmpty()) extraProps.toMap() else emptyMap()

  /** Empty constructor with no extra properties. */
  public constructor(): this(emptyArray())

  /**
   * String message associated with this error, associated at construction time.
   */
  @get:Polyglot public abstract override val message: String

  /**
   * Name of the cause or type of this error, if any.
   */
  @get:Polyglot public abstract val name: String

  /**
   * Error number assigned to this error (also known as the error code).
   */
  @get:Polyglot public open val errno: Int? get() = null

  /**
   * Causing [Error] which this error wraps, if any.
   */
  @get:Polyglot public override val cause: Error? get() = null

  /**
   * File name where the error was thrown, if any or if known.
   */
  @get:Polyglot public open val fileName: String? get() = null

  /**
   * Line number where the error was thrown, if any or if known.
   */
  @get:Polyglot public open val lineNumber: Int? get() = null

  /**
   * Column number where the error was thrown, if any or if known.
   */
  @get:Polyglot public open val columnNumber: Int? get() = null

  /**
   * Stack trace for this error.
   */
  @get:Polyglot public val stackTrace: Stacktrace get() {
    TODO("not yet implemented")
  }

  override fun getMember(key: String?): Any? = when (key) {
    "message" -> message
    "name" -> name
    "errno" -> errno
    "cause" -> cause
    "fileName" -> fileName
    "lineNumber" -> lineNumber
    "columnNumber" -> columnNumber
    "stackTrace" -> stackTrace
    in extraProps -> extraProps[key]
    else -> null
  }

  override fun getMemberKeys(): Array<String> = JS_ERROR_PROPS_AND_METHODS
  override fun hasMember(key: String?): Boolean = key != null && key in JS_ERROR_PROPS_AND_METHODS

  override fun putMember(key: String?, value: Value?) {
    // no-op
  }
}
