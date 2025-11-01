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
import elide.lang.php.runtime.PhpReference;

/** Built-in function: implode Joins array elements with a string. implode(glue, array) */
public final class ImplodeBuiltin extends PhpBuiltinRootNode {

  public ImplodeBuiltin(PhpLanguage language) {
    super(language, "implode");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length < 2) {
      return "";
    }

    String glue = args[0].toString();
    Object arrayArg = args[1];

    if (!(arrayArg instanceof PhpArray)) {
      return "";
    }

    PhpArray array = (PhpArray) arrayArg;
    StringBuilder result = new StringBuilder();
    boolean first = true;

    for (Object key : array.keys()) {
      if (!first) {
        result.append(glue);
      }
      Object value = array.get(key);
      // Unwrap PhpReference if present
      if (value instanceof PhpReference) {
        value = ((PhpReference) value).getValue();
      }
      result.append(value.toString());
      first = false;
    }

    return result.toString();
  }
}
