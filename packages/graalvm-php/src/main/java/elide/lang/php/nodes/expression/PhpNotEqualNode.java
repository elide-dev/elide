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

/** Node for not-equal comparison (!=) in PHP. */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpNotEqualNode extends PhpExpressionNode {

  @Child protected PhpExpressionNode left;
  @Child protected PhpExpressionNode right;

  protected PhpNotEqualNode(PhpExpressionNode left, PhpExpressionNode right) {
    this.left = left;
    this.right = right;
  }

  public static PhpNotEqualNode create(PhpExpressionNode left, PhpExpressionNode right) {
    return new PhpNotEqualNodeImpl(left, right);
  }

  static final class PhpNotEqualNodeImpl extends PhpNotEqualNode {
    PhpNotEqualNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
      super(left, right);
    }

    @Override
    public Object execute(VirtualFrame frame) {
      Object leftVal = left.execute(frame);
      Object rightVal = right.execute(frame);

      if (leftVal == null && rightVal == null) {
        return false;
      } else if (leftVal == null || rightVal == null) {
        return true;
      } else if (leftVal instanceof Long && rightVal instanceof Long) {
        return !leftVal.equals(rightVal);
      } else if (leftVal instanceof Double && rightVal instanceof Double) {
        return !leftVal.equals(rightVal);
      } else if (leftVal instanceof Long && rightVal instanceof Double) {
        return ((Long) leftVal).doubleValue() != (Double) rightVal;
      } else if (leftVal instanceof Double && rightVal instanceof Long) {
        return (Double) leftVal != ((Long) rightVal).doubleValue();
      } else if (leftVal instanceof Boolean && rightVal instanceof Boolean) {
        return !leftVal.equals(rightVal);
      } else if (leftVal instanceof String && rightVal instanceof String) {
        return !leftVal.equals(rightVal);
      }

      return !leftVal.equals(rightVal);
    }
  }
}
