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
package elide.tooling

/**
 * ## Argument Context
 *
 * Defines the parameters which are made available during argument rendering, in order to render arguments to strings
 * in a suitable form for a given host operating system or tool invocation style.
 */
public interface ArgumentContext {
  public val argSeparator: Char
  public val kvToken: Char

  /**
   * ### Default argument context.
   *
   * Defines an argument context which uses sensible tokens to join arguments and key/value strings.
   */
  public data object Default : ArgumentContext {
    override val argSeparator: Char get() = ' '
    override val kvToken: Char get() = '='
  }

  /** Factories for obtaining [ArgumentContext] instances. */
  public companion object {
    /** @return Custom argument context. */
    @JvmStatic public fun of(argSeparator: Char, kvToken: Char): ArgumentContext = object : ArgumentContext {
      override val argSeparator: Char get() = argSeparator
      override val kvToken: Char get() = kvToken
    }
  }
}
