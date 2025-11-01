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

/**
 * Built-in function: array_slice Extract a slice of the array. Usage: array_slice(array, offset,
 * length = null)
 */
public final class ArraySliceBuiltin extends PhpBuiltinRootNode {

  public ArraySliceBuiltin(PhpLanguage language) {
    super(language, "array_slice");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length < 2) {
      throw new RuntimeException("array_slice() expects at least 2 parameters");
    }

    if (!(args[0] instanceof PhpArray)) {
      throw new RuntimeException("array_slice() expects parameter 1 to be array");
    }

    PhpArray array = (PhpArray) args[0];
    long offset = convertToLong(args[1]);
    Long length = args.length >= 3 && args[2] != null ? convertToLong(args[2]) : null;

    List<Object> keys = array.keys();
    int size = keys.size();

    // Handle negative offset (count from end)
    int startIndex;
    if (offset < 0) {
      startIndex = (int) Math.max(0, size + offset);
    } else {
      startIndex = (int) Math.min(offset, size);
    }

    // Determine end index
    int endIndex;
    if (length == null) {
      endIndex = size;
    } else if (length < 0) {
      endIndex = (int) Math.max(startIndex, size + length);
    } else {
      endIndex = (int) Math.min(startIndex + length, size);
    }

    // Create result array with sliced elements
    PhpArray result = new PhpArray();
    for (int i = startIndex; i < endIndex; i++) {
      Object key = keys.get(i);
      Object value = array.get(key);
      result.append(value);
    }

    return result;
  }

  private long convertToLong(Object obj) {
    if (obj instanceof Long) {
      return (Long) obj;
    } else if (obj instanceof Double) {
      return ((Double) obj).longValue();
    } else if (obj instanceof String) {
      try {
        return Long.parseLong((String) obj);
      } catch (NumberFormatException e) {
        return 0L;
      }
    }
    return 0L;
  }
}
