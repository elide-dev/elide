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

/** Node for logical OR (||) in PHP. Uses short-circuit evaluation. */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpLogicalOrNode extends PhpExpressionNode {

  @Child protected PhpExpressionNode left;
  @Child protected PhpExpressionNode right;

  protected PhpLogicalOrNode(PhpExpressionNode left, PhpExpressionNode right) {
    this.left = left;
    this.right = right;
  }

  public static PhpLogicalOrNode create(PhpExpressionNode left, PhpExpressionNode right) {
    return new PhpLogicalOrNodeImpl(left, right);
  }

  static final class PhpLogicalOrNodeImpl extends PhpLogicalOrNode {
    PhpLogicalOrNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
      super(left, right);
    }

    @Override
    public Object execute(VirtualFrame frame) {
      Object leftVal = left.execute(frame);

      // Short-circuit: if left is true, don't evaluate right
      if (isTruthy(leftVal)) {
        return true;
      }

      Object rightVal = right.execute(frame);
      return isTruthy(rightVal);
    }

    private boolean isTruthy(Object value) {
      if (value == null) {
        return false;
      } else if (value instanceof Boolean) {
        return (Boolean) value;
      } else if (value instanceof Long) {
        return (Long) value != 0;
      } else if (value instanceof Double) {
        return (Double) value != 0.0;
      } else if (value instanceof String) {
        String str = (String) value;
        return !str.isEmpty() && !str.equals("0");
      }
      return true;
    }
  }
}
