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
import com.oracle.truffle.api.nodes.NodeInfo;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.types.PhpTypes;

/**
 * AST node for ternary conditional operator (condition ? trueValue : falseValue). Implements
 * short-circuit evaluation - only the selected branch is evaluated.
 */
@NodeInfo(shortName = "?:")
public final class PhpTernaryNode extends PhpExpressionNode {

  @Child private PhpExpressionNode conditionNode;

  @Child private PhpExpressionNode trueNode;

  @Child private PhpExpressionNode falseNode;

  public PhpTernaryNode(
      PhpExpressionNode conditionNode, PhpExpressionNode trueNode, PhpExpressionNode falseNode) {
    this.conditionNode = conditionNode;
    this.trueNode = trueNode;
    this.falseNode = falseNode;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object conditionValue = conditionNode.execute(frame);

    // Convert condition to boolean using PHP truthiness rules
    boolean condition = PhpTypes.toBoolean(conditionValue);

    if (condition) {
      return trueNode.execute(frame);
    } else {
      return falseNode.execute(frame);
    }
  }
}
