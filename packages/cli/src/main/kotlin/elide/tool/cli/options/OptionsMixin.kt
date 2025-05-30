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

package elide.tool.cli.options

/**
 * # Options Mixins
 *
 * Defines a set of build-time-declared command line options, which are grouped by relevance in a class and mixed into
 * commands downstream.
 */
sealed interface OptionsMixin<T> where T: OptionsMixin<T> {
  /** @return Merged options with the [other] instance. */
  @Suppress("UNCHECKED_CAST") fun merge(other: T? = null): T {
    return this as T // (self)
  }
}
