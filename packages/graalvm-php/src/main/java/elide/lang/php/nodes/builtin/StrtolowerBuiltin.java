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

/** Built-in function: strtolower Converts a string to lowercase. */
public final class StrtolowerBuiltin extends PhpBuiltinRootNode {

  public StrtolowerBuiltin(PhpLanguage language) {
    super(language, "strtolower");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length == 0) {
      return "";
    }
    return args[0].toString().toLowerCase();
  }
}
