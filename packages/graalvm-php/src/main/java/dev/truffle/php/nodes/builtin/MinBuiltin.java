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

/** Built-in function: min Returns the minimum value. */
public final class MinBuiltin extends PhpBuiltinRootNode {

  public MinBuiltin(PhpLanguage language) {
    super(language, "min");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length == 0) {
      return null;
    }

    Object min = args[0];
    for (int i = 1; i < args.length; i++) {
      if (compare(args[i], min) < 0) {
        min = args[i];
      }
    }

    return min;
  }

  private int compare(Object a, Object b) {
    if (a instanceof Long && b instanceof Long) {
      return Long.compare((Long) a, (Long) b);
    } else if (a instanceof Double || b instanceof Double) {
      double da = toDouble(a);
      double db = toDouble(b);
      return Double.compare(da, db);
    }
    return 0;
  }

  private double toDouble(Object obj) {
    if (obj instanceof Long) return ((Long) obj).doubleValue();
    if (obj instanceof Double) return (Double) obj;
    return 0.0;
  }
}
