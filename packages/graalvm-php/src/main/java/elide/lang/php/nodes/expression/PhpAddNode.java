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
 * Node for addition operation in PHP. Uses Truffle DSL for automatic specialization and type
 * handling.
 */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpAddNode extends PhpExpressionNode {

  @Child protected PhpExpressionNode left;
  @Child protected PhpExpressionNode right;

  protected PhpAddNode(PhpExpressionNode left, PhpExpressionNode right) {
    this.left = left;
    this.right = right;
  }

  public static PhpAddNode create(PhpExpressionNode left, PhpExpressionNode right) {
    return new PhpAddNodeImpl(left, right);
  }

  static final class PhpAddNodeImpl extends PhpAddNode {
    PhpAddNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
      super(left, right);
    }

    @Override
    public Object execute(VirtualFrame frame) {
      Object leftVal = left.execute(frame);
      Object rightVal = right.execute(frame);

      if (leftVal instanceof Long && rightVal instanceof Long) {
        try {
          return Math.addExact((Long) leftVal, (Long) rightVal);
        } catch (ArithmeticException e) {
          return (double) (Long) leftVal + (double) (Long) rightVal;
        }
      } else if (leftVal instanceof Double && rightVal instanceof Double) {
        return (Double) leftVal + (Double) rightVal;
      } else if (leftVal instanceof Long && rightVal instanceof Double) {
        return (Long) leftVal + (Double) rightVal;
      } else if (leftVal instanceof Double && rightVal instanceof Long) {
        return (Double) leftVal + (Long) rightVal;
      }

      return 0L;
    }
  }
}
