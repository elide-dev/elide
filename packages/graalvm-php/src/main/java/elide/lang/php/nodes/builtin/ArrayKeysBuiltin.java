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

/** Built-in function: array_keys Returns all the keys of an array. */
public final class ArrayKeysBuiltin extends PhpBuiltinRootNode {

  public ArrayKeysBuiltin(PhpLanguage language) {
    super(language, "array_keys");
  }

  @Override
  @TruffleBoundary
  protected Object executeBuiltin(Object[] args) {
    if (args.length == 0) {
      return new PhpArray();
    }

    Object arrayArg = args[0];
    if (!(arrayArg instanceof PhpArray)) {
      return new PhpArray();
    }

    PhpArray array = (PhpArray) arrayArg;
    PhpArray result = new PhpArray();

    for (Object key : array.keys()) {
      result.append(key);
    }

    return result;
  }
}
