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

package elide.runtime.core.extensions

import org.graalvm.polyglot.management.ExecutionListener
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.internals.graalvm.GraalVMEngine

/**
 * Attaches this [builder][ExecutionListener.Builder] to a GraalVM-backed [PolyglotEngine].
 *
 * @param engine A [PolyglotEngine] to attach this listener to.
 * @throws IllegalArgumentException If the provided [engine] is not backed by a GraalVM implementation and does not
 * otherwise support execution listeners.
 */
@DelicateElideApi public fun ExecutionListener.Builder.attach(engine: PolyglotEngine): ExecutionListener {
  require(engine is GraalVMEngine) { "The provided engine does not support GraalVM execution listeners." }
  return attach(engine.unwrap())
}
