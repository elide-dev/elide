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

/**
 * Built-in function: array_push Pushes one or more elements onto the end of an array. Returns the
 * new array size.
 */
public final class ArrayPushBuiltin extends PhpBuiltinRootNode {

  public ArrayPushBuiltin(PhpLanguage language) {
    super(language, "array_push");
  }

  @Override
  @TruffleBoundary
  protected Object executeBuiltin(Object[] args) {
    if (args.length < 2) {
      return 0L;
    }

    Object arrayArg = args[0];
    if (!(arrayArg instanceof PhpArray)) {
      return 0L;
    }

    PhpArray array = (PhpArray) arrayArg;
    for (int i = 1; i < args.length; i++) {
      array.append(args[i]);
    }

    return (long) array.size();
  }
}
