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

package elide.runtime.plugins.python

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.core.evaluate

/**
 * Execute the given Python [source] code, returning the result. This is equivalent to calling
 * [PolyglotContext.evaluate] and selecting [Python] as source language.
 *
 * @param source The Python source code to be executed.
 * @return The result of the invocation.
 */
@DelicateElideApi public fun PolyglotContext.python(
  /*language=python*/ source: String,
  name: String? = null,
): PolyglotValue {
  return evaluate(Python, source, name = name)
}
