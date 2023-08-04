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

package elide.server

/** General utilities for use in Elide tests. */
public object TestUtil {
  /**
   * Load a text fixture file for a test, as a string, from a classpath resource.
   *
   * @param name Name of the resource to load
   * @return File contents.
   */
  public fun loadFixture(name: String): String = requireNotNull(TestUtil::class.java.getResourceAsStream(
    name
  )).bufferedReader().use {
    it.readText()
  }

  /**
   * Load a binary fixture from a file for a test, as a string, from a classpath resource.
   *
   * @param name Name of the resource to load
   * @return File contents.
   */
  public fun loadBinary(name: String): ByteArray = requireNotNull(TestUtil::class.java.getResourceAsStream(
    name
  )).buffered().use {
    it.readAllBytes()
  }
}
