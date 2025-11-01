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
import java.util.List;
import java.util.Objects;

/**
 * Built-in function: array_search Searches the array for a given value and returns the first
 * corresponding key if successful. Usage: array_search(needle, haystack) Returns the key or false
 * if not found.
 */
public final class ArraySearchBuiltin extends PhpBuiltinRootNode {

  public ArraySearchBuiltin(PhpLanguage language) {
    super(language, "array_search");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length < 2) {
      throw new RuntimeException("array_search() expects at least 2 parameters");
    }

    Object needle = args[0];

    if (!(args[1] instanceof PhpArray)) {
      throw new RuntimeException("array_search() expects parameter 2 to be array");
    }

    PhpArray haystack = (PhpArray) args[1];
    List<Object> keys = haystack.keys();

    // Search for the needle in the array
    for (Object key : keys) {
      Object value = haystack.get(key);
      if (Objects.equals(value, needle)) {
        return key;
      }
    }

    // Not found - return false
    return false;
  }
}
