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
import elide.lang.php.runtime.PhpContext;
import java.io.PrintWriter;

/** Built-in function: var_dump Dumps information about a variable. */
public final class VarDumpBuiltin extends PhpBuiltinRootNode {

  public VarDumpBuiltin(PhpLanguage language) {
    super(language, "var_dump");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    PrintWriter out = new PrintWriter(PhpContext.get(null).getOutput(), true);

    for (Object arg : args) {
      dumpValue(out, arg, 0);
    }

    return null;
  }

  private void dumpValue(PrintWriter out, Object value, int indent) {
    if (value == null) {
      out.println("NULL");
    } else if (value instanceof Boolean) {
      out.println("bool(" + value + ")");
    } else if (value instanceof Long) {
      out.println("int(" + value + ")");
    } else if (value instanceof Double) {
      out.println("float(" + value + ")");
    } else if (value instanceof String) {
      String str = (String) value;
      out.println("string(" + str.length() + ") \"" + str + "\"");
    } else if (value instanceof PhpArray) {
      PhpArray array = (PhpArray) value;
      out.println("array(" + array.size() + ") {");
      for (Object key : array.keys()) {
        out.print("  [" + key + "]=>\n  ");
        dumpValue(out, array.get(key), indent + 2);
      }
      out.println("}");
    } else {
      out.println(value.getClass().getSimpleName() + "(" + value + ")");
    }
  }
}
