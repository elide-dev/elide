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

package elide.tools.processor.util

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueArgument

// Return a predicate which finds an annotation parameter by name and type.
private inline fun <reified T : Any> matchParameterPredicate(name: String): (KSValueArgument) -> Boolean {
  return {
    // match by name
    it.name?.getShortName() == name &&

    // then by value and value class
    when (val inner = it.value) {
      null -> true
      else -> when (inner) {
        is T -> true
        else -> false
      }
    }
  }
}

/**
 *
 */
internal inline fun <reified T : Any> SymbolProcessor.annotationArgument(
  name: String,
  anno: KSAnnotation,
  defaultValue: T? = null,
): T? {
  val found = anno.arguments.find(
    matchParameterPredicate<T>(
    name,
  ),
  )

  // resolve typed value, fail if cast fails, but only if not null
  return when (val inner = found?.value) {
    null -> anno.defaultArguments.find(
      matchParameterPredicate<T>(
      name,
    ),
    )?.value as? T ?: defaultValue

    else -> inner as? T ?: error(
      "Failed to cast annotation argument '$name' as expected type '${T::class.simpleName}'",
    )
  } ?: defaultValue
}

/**
 *
 */
internal inline fun <reified T : Any> SymbolProcessor.annotationArgumentWithDefault(
  name: String,
  anno: KSAnnotation,
): T {
  return anno.arguments.find(
    matchParameterPredicate<T>(
    name,
  ),
  )?.value as? T ?: anno.defaultArguments.find(
    matchParameterPredicate<T>(
    name,
  ),
  )?.value as? T ?: error(
    "Failed to resolve annotation '$name' with expected default value",
  )
}
