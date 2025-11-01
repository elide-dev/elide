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

/**
 * Built-in function: array_reverse Return an array with elements in reverse order. Usage:
 * array_reverse(array)
 */
public final class ArrayReverseBuiltin extends PhpBuiltinRootNode {

  public ArrayReverseBuiltin(PhpLanguage language) {
    super(language, "array_reverse");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length < 1) {
      throw new RuntimeException("array_reverse() expects at least 1 parameter");
    }

    if (!(args[0] instanceof PhpArray)) {
      throw new RuntimeException("array_reverse() expects parameter 1 to be array");
    }

    PhpArray array = (PhpArray) args[0];
    List<Object> keys = array.keys();

    // Create result array with reversed elements
    PhpArray result = new PhpArray();
    for (int i = keys.size() - 1; i >= 0; i--) {
      Object key = keys.get(i);
      Object value = array.get(key);
      result.append(value);
    }

    return result;
  }
}
