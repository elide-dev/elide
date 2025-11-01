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
 * AST node for unset() language construct. Unsets one or more variables by setting them to null. In
 * PHP, unset() is technically a statement, but we treat it as an expression that returns null for
 * simplicity.
 */
public final class PhpUnsetNode extends PhpExpressionNode {

  private final int[] variableSlots;

  public PhpUnsetNode(int[] variableSlots) {
    this.variableSlots = variableSlots;
  }

  @Override
  @ExplodeLoop
  public Object execute(VirtualFrame frame) {
    // Unset all specified variables by setting them to null
    for (int slot : variableSlots) {
      frame.setObject(slot, null);
    }
    // unset() doesn't return a value, but we return null for consistency
    return null;
  }
}
