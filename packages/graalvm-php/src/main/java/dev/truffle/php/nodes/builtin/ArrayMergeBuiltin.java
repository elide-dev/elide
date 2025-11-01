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
package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/** Built-in function: array_merge Merges one or more arrays. */
public final class ArrayMergeBuiltin extends PhpBuiltinRootNode {

  public ArrayMergeBuiltin(PhpLanguage language) {
    super(language, "array_merge");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    PhpArray result = new PhpArray();

    for (Object arg : args) {
      if (arg instanceof PhpArray) {
        PhpArray array = (PhpArray) arg;
        for (Object key : array.keys()) {
          result.append(array.get(key));
        }
      }
    }

    return result;
  }
}
