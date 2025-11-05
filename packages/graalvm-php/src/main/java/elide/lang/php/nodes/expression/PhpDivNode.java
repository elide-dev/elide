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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.nodes.PhpExpressionNode;

/**
 * Node for division operation in PHP. PHP division always returns a float if there's a remainder.
 */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpDivNode extends PhpExpressionNode {

  @Child protected PhpExpressionNode left;
  @Child protected PhpExpressionNode right;

  protected PhpDivNode(PhpExpressionNode left, PhpExpressionNode right) {
    this.left = left;
    this.right = right;
  }

  public static PhpDivNode create(PhpExpressionNode left, PhpExpressionNode right) {
    return new PhpDivNodeImpl(left, right);
  }

  static final class PhpDivNodeImpl extends PhpDivNode {
    PhpDivNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
      super(left, right);
    }

    @Override
    public Object execute(VirtualFrame frame) {
      Object leftVal = left.execute(frame);
      Object rightVal = right.execute(frame);

      if (leftVal instanceof Long && rightVal instanceof Long) {
        long leftLong = (Long) leftVal;
        long rightLong = (Long) rightVal;
        if (rightLong == 0) {
          return Double.POSITIVE_INFINITY;
        }
        if (leftLong % rightLong == 0) {
          return leftLong / rightLong;
        }
        return (double) leftLong / (double) rightLong;
      } else if (leftVal instanceof Double && rightVal instanceof Double) {
        return (Double) leftVal / (Double) rightVal;
      } else if (leftVal instanceof Long && rightVal instanceof Double) {
        return (Long) leftVal / (Double) rightVal;
      } else if (leftVal instanceof Double && rightVal instanceof Long) {
        return (Double) leftVal / (Long) rightVal;
      }

      return Double.NaN;
    }
  }
}
