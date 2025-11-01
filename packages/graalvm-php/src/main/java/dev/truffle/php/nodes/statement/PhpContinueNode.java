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
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpContinueException;

/**
 * Node for continue statement in PHP. Throws PhpContinueException to skip to the next iteration of
 * loops.
 */
public final class PhpContinueNode extends PhpStatementNode {

  private final int level;

  public PhpContinueNode(int level) {
    this.level = level;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    throw new PhpContinueException(level);
  }
}
