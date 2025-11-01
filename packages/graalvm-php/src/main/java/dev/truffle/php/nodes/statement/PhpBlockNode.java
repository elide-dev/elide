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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpStatementNode;

/** Node representing a block of statements. Executes multiple statements in sequence. */
public final class PhpBlockNode extends PhpStatementNode {

  @Children private final PhpStatementNode[] statements;

  public PhpBlockNode(PhpStatementNode[] statements) {
    this.statements = statements;
  }

  @Override
  @ExplodeLoop
  public void executeVoid(VirtualFrame frame) {
    for (PhpStatementNode statement : statements) {
      statement.executeVoid(frame);
    }
  }
}
