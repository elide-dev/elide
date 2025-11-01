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
import com.oracle.truffle.api.nodes.NodeInfo;
import elide.lang.php.nodes.PhpExpressionNode;

/**
 * AST node for null coalescing operator (leftExpr ?? rightExpr). Returns leftExpr if it is not
 * null, otherwise returns rightExpr. Implements short-circuit evaluation - rightExpr is only
 * evaluated if leftExpr is null.
 */
@NodeInfo(shortName = "??")
public final class PhpNullCoalescingNode extends PhpExpressionNode {

  @Child private PhpExpressionNode leftNode;

  @Child private PhpExpressionNode rightNode;

  public PhpNullCoalescingNode(PhpExpressionNode leftNode, PhpExpressionNode rightNode) {
    this.leftNode = leftNode;
    this.rightNode = rightNode;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object leftValue = leftNode.execute(frame);

    // If left value is not null, return it
    if (leftValue != null) {
      return leftValue;
    }

    // Otherwise, evaluate and return right value
    return rightNode.execute(frame);
  }
}
