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
package elide.tooling.testing

import org.graalvm.polyglot.Value as PolyglotValue

/**
 * ## Test Scope
 *
 * Describes, in a sealed hierarchy, all scopes which apply to testing; this includes [RegisteredScope] instances
 * which describe logical groupings of tests, and also tests themselves, via [RegisteredTest].
 */
public interface TestScope<T>: Comparable<T> where T: TestScope<T> {
  /**
   * ### Simple name.
   *
   * Every test and test scope provides a simple display name.
   */
  public val simpleName: String

  /**
   * ### Qualified name.
   *
   * Every test and test scope provides a well-qualified name.
   */
  public val qualifiedName: String

  /**
   * Return a generated qualified test name based on this scope and any parent scopes.
   *
   * @param block Block to generate a qualified name for.
   * @param label Optional label to append to the qualified name.
   * @return Qualified name for the test.
   */
  public fun qualifiedNameFor(block: PolyglotValue, label: String?): String {
    if (qualifiedName.isEmpty() || qualifiedName.isBlank()) {
      val srcfile = block.sourceLocation.source.name
      return "$srcfile > ${label ?: block.metaSimpleName}"
    }
    return qualifiedName + (label?.let { " > $it" } ?: "")
  }

  /**
   * Return a generated qualified test name based on this scope and any parent scopes.
   *
   * @param label Optional label to append to the qualified name.
   * @return Qualified name for the test.
   */
  public fun qualifiedNameFor(label: String?): String? {
    if (qualifiedName.isNotEmpty() && label?.isNotEmpty() == true) {
      return qualifiedName + label.let { " > $it" }
    }
    return null
  }

  override fun compareTo(other: T): Int {
    return qualifiedName.compareTo(other.qualifiedName)
  }
}
