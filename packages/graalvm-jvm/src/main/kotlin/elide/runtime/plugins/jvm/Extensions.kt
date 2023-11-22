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

package elide.runtime.plugins.jvm

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext

/**
 * Run the entrypoint of a guest JVM application. This extension attempts to locate a static method with the
 * specified [mainMethodName] in a class defined by a fully-qualified [mainClassName].
 *
 * A class with the specified [mainClassName] must be present in the guest classpath, as configured by the [JvmConfig]
 * during the construction of the engine.
 *
 * The resolved method must include a single parameter in its signature, with type `Array<String>`, otherwise this call
 * will fail.
 *
 * @see Jvm
 */
@DelicateElideApi public fun PolyglotContext.runJvm(
  mainClassName: String,
  mainMethodName: String = "main",
  arguments: Array<String> = arrayOf(),
): Int {
  // attempt to locate the main class
  val mainClass = bindings(Jvm).getMember(mainClassName) ?: error(
    "Class <$mainClassName> not found in guest context, check your guest classpath configuration",
  )

  // run the entrypoint and encapsulate errors
  val exitCode = runCatching { mainClass.invokeMember(mainMethodName, arguments) }.getOrElse { cause ->
    error("Error during guest execution: $cause")
  }

  // treat void/null return values as a 0 exit code
  if (exitCode == null || exitCode.isNull) return 0

  // unwrap explicit exit code
  check(exitCode.isNumber && exitCode.fitsInInt()) { "Expected return value to be Int, found $exitCode" }
  return exitCode.asInt()
}

