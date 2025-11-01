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

/** Built-in function: gettype Returns the type of a variable as a string. */
public final class GettypeBuiltin extends PhpBuiltinRootNode {

  public GettypeBuiltin(PhpLanguage language) {
    super(language, "gettype");
  }

  @Override
  @TruffleBoundary
  protected Object executeBuiltin(Object[] args) {
    if (args.length == 0 || args[0] == null) {
      return "NULL";
    }

    Object arg = args[0];
    if (arg instanceof Long) {
      return "integer";
    } else if (arg instanceof Double) {
      return "double";
    } else if (arg instanceof String) {
      return "string";
    } else if (arg instanceof Boolean) {
      return "boolean";
    } else if (arg instanceof PhpArray) {
      return "array";
    } else {
      return "unknown type";
    }
  }
}
