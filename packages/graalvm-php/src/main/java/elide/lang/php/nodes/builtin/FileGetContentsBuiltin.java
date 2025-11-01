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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Built-in function: file_get_contents Reads entire file into a string.
 *
 * <p>Usage: $content = file_get_contents('path/to/file.txt');
 */
public final class FileGetContentsBuiltin extends PhpBuiltinRootNode {

  public FileGetContentsBuiltin(PhpLanguage language) {
    super(language, "file_get_contents");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length == 0) {
      throw new RuntimeException("file_get_contents() expects at least 1 parameter, 0 given");
    }

    Object fileArg = args[0];
    if (!(fileArg instanceof String)) {
      throw new RuntimeException(
          "file_get_contents() expects parameter 1 to be string, " + getType(fileArg) + " given");
    }

    String filename = (String) fileArg;

    try {
      Path path = Paths.get(filename);
      byte[] bytes = Files.readAllBytes(path);
      return new String(bytes, "UTF-8");
    } catch (IOException e) {
      // PHP returns false on failure
      return false;
    }
  }

  private String getType(Object value) {
    if (value == null) return "null";
    if (value instanceof String) return "string";
    if (value instanceof Long) return "integer";
    if (value instanceof Double) return "double";
    if (value instanceof Boolean) return "boolean";
    return "unknown";
  }
}
