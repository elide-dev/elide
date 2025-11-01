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
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Built-in function: dirname Returns a parent directory's path.
 *
 * <p>Usage: $dir = dirname('/path/to/file.txt'); // Returns '/path/to'
 */
public final class DirnameBuiltin extends PhpBuiltinRootNode {

  public DirnameBuiltin(PhpLanguage language) {
    super(language, "dirname");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length == 0) {
      throw new RuntimeException("dirname() expects at least 1 parameter, 0 given");
    }

    Object pathArg = args[0];
    if (!(pathArg instanceof String)) {
      return "";
    }

    String pathname = (String) pathArg;

    try {
      Path path = Paths.get(pathname);
      Path parent = path.getParent();
      if (parent == null) {
        // No parent directory (e.g., relative path like "file.txt" or root "/")
        return ".";
      }
      return parent.toString();
    } catch (Exception e) {
      return ".";
    }
  }
}
