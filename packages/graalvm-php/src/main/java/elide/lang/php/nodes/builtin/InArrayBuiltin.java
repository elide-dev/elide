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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import elide.lang.php.PhpLanguage;
import elide.lang.php.nodes.PhpBuiltinRootNode;
import elide.lang.php.runtime.PhpArray;

/** Built-in function: in_array Checks if a value exists in an array. */
public final class InArrayBuiltin extends PhpBuiltinRootNode {

  public InArrayBuiltin(PhpLanguage language) {
    super(language, "in_array");
  }

  @Override
  @TruffleBoundary
  protected Object executeBuiltin(Object[] args) {
    if (args.length < 2) {
      return false;
    }

    Object needle = args[0];
    Object haystackArg = args[1];

    if (!(haystackArg instanceof PhpArray)) {
      return false;
    }

    PhpArray haystack = (PhpArray) haystackArg;
    for (Object key : haystack.keys()) {
      Object value = haystack.get(key);
      if (equals(needle, value)) {
        return true;
      }
    }

    return false;
  }

  private boolean equals(Object a, Object b) {
    if (a == b) return true;
    if (a == null || b == null) return false;
    return a.equals(b);
  }
}
