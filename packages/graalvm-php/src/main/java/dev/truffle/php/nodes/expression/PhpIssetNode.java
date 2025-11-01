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
package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * AST node for isset() language construct. Checks if one or more variables are set and not null.
 * Returns true only if ALL variables are set and not null.
 */
public final class PhpIssetNode extends PhpExpressionNode {

  private final int[] variableSlots;

  public PhpIssetNode(int[] variableSlots) {
    this.variableSlots = variableSlots;
  }

  @Override
  @ExplodeLoop
  public Object execute(VirtualFrame frame) {
    // isset returns true only if all variables are set and not null
    for (int slot : variableSlots) {
      try {
        Object value = frame.getObject(slot);
        if (value == null) {
          return false;
        }
      } catch (Exception e) {
        // Variable not initialized
        return false;
      }
    }
    return true;
  }
}
