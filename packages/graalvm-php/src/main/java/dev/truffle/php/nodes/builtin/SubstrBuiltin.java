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

/** Built-in function: substr Returns a portion of a string. substr(string, start, length) */
public final class SubstrBuiltin extends PhpBuiltinRootNode {

  public SubstrBuiltin(PhpLanguage language) {
    super(language, "substr");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length < 2) {
      return null;
    }

    String str = toString(args[0]);
    int start = toInt(args[1]);
    int length = args.length > 2 ? toInt(args[2]) : str.length();

    // Handle negative start
    if (start < 0) {
      start = str.length() + start;
    }

    // Bounds check
    if (start < 0 || start >= str.length()) {
      return "";
    }

    int end = Math.min(start + length, str.length());
    return str.substring(start, end);
  }

  private String toString(Object obj) {
    if (obj == null) return "";
    return obj.toString();
  }

  private int toInt(Object obj) {
    if (obj instanceof Long) {
      return ((Long) obj).intValue();
    }
    if (obj instanceof Double) {
      return ((Double) obj).intValue();
    }
    return 0;
  }
}
