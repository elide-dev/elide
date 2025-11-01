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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Built-in function: is_dir Checks whether the given path is a directory.
 *
 * <p>Usage: if (is_dir('path/to/directory')) { ... }
 */
public final class IsDirBuiltin extends PhpBuiltinRootNode {

  public IsDirBuiltin(PhpLanguage language) {
    super(language, "is_dir");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length == 0) {
      throw new RuntimeException("is_dir() expects at least 1 parameter, 0 given");
    }

    Object fileArg = args[0];
    if (!(fileArg instanceof String)) {
      return false;
    }

    String filename = (String) fileArg;

    try {
      Path path = Paths.get(filename);
      return Files.isDirectory(path);
    } catch (Exception e) {
      return false;
    }
  }
}
