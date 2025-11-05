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
package elide.lang.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.nodes.types.PhpTypes;

/**
 * AST node for empty() language construct. Checks if a variable is empty according to PHP's empty
 * rules. Returns true if the variable is unset or has an "empty" value.
 *
 * <p>Empty values in PHP: - null - false - 0 (integer) - 0.0 (float) - "" (empty string) - "0"
 * (string containing zero) - empty array
 */
public final class PhpEmptyNode extends PhpExpressionNode {

  private final int variableSlot;

  public PhpEmptyNode(int variableSlot) {
    this.variableSlot = variableSlot;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    try {
      Object value = frame.getObject(variableSlot);
      return PhpTypes.isEmpty(value);
    } catch (Exception e) {
      // Variable not initialized - counts as empty
      return true;
    }
  }
}
