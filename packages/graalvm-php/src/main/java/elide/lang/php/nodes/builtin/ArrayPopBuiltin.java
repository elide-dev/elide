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
package elide.lang.php.nodes.builtin;

import elide.lang.php.PhpLanguage;
import elide.lang.php.nodes.PhpBuiltinRootNode;
import elide.lang.php.runtime.PhpArray;
import java.util.List;

/** Built-in function: array_pop Pops and returns the last element from an array. */
public final class ArrayPopBuiltin extends PhpBuiltinRootNode {

  public ArrayPopBuiltin(PhpLanguage language) {
    super(language, "array_pop");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length == 0) {
      return null;
    }

    Object arrayArg = args[0];
    if (!(arrayArg instanceof PhpArray)) {
      return null;
    }

    PhpArray array = (PhpArray) arrayArg;
    List<Object> keys = array.keys();
    if (keys.isEmpty()) {
      return null;
    }

    // Get last key and remove it
    Object lastKey = keys.get(keys.size() - 1);
    Object value = array.get(lastKey);
    array.remove(lastKey);

    return value;
  }
}
