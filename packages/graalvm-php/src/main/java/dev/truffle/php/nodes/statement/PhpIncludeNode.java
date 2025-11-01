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
package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * AST node for include statement. Includes and executes a PHP file. Returns false if file not found
 * (warning, not fatal error).
 */
public final class PhpIncludeNode extends PhpStatementNode {

  @Child private PhpExpressionNode pathNode;

  private final boolean once;

  public PhpIncludeNode(PhpExpressionNode pathNode, boolean once) {
    this.pathNode = pathNode;
    this.once = once;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    Object pathValue = pathNode.execute(frame);
    String path = pathValue.toString();

    PhpContext context = PhpContext.get(this);
    TruffleLanguage.Env env = context.getEnv();

    // Check if already included for include_once
    if (once && context.isFileIncluded(path)) {
      return;
    }

    // Resolve the file using Truffle's sandboxed I/O
    TruffleFile file = env.getPublicTruffleFile(path);

    // Check if file exists - for include, print warning instead of throwing
    if (!file.exists()) {
      // In real PHP this would emit a warning
      System.err.println(
          "Warning: include(" + path + "): Failed to open stream: No such file or directory");
      return;
    }

    // Check if it's a regular file
    if (!file.isRegularFile()) {
      System.err.println("Warning: include(" + path + "): Not a regular file");
      return;
    }

    try {
      // Read the file content
      StringBuilder content = new StringBuilder();
      try (BufferedReader reader = file.newBufferedReader(StandardCharsets.UTF_8)) {
        String line;
        while ((line = reader.readLine()) != null) {
          content.append(line).append("\n");
        }
      }

      // Create a Source from the file
      Source source =
          Source.newBuilder(PhpLanguage.ID, content.toString(), file.getName())
              .uri(file.toUri())
              .build();

      // Mark file as included for _once variants
      if (once) {
        context.markFileIncluded(path);
      }

      // Parse and execute the file
      PhpLanguage language = PhpLanguage.get(this);
      language.parseAndExecute(source, frame);

    } catch (IOException e) {
      System.err.println("Warning: include(" + path + "): Failed to read file: " + e.getMessage());
    }
  }
}
