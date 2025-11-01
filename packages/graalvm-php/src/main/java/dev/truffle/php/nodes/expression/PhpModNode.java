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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/** Node for modulo operation in PHP. */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpModNode extends PhpExpressionNode {

  @Child protected PhpExpressionNode left;
  @Child protected PhpExpressionNode right;

  protected PhpModNode(PhpExpressionNode left, PhpExpressionNode right) {
    this.left = left;
    this.right = right;
  }

  public static PhpModNode create(PhpExpressionNode left, PhpExpressionNode right) {
    return new PhpModNodeImpl(left, right);
  }

  static final class PhpModNodeImpl extends PhpModNode {
    PhpModNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
      super(left, right);
    }

    @Override
    public Object execute(VirtualFrame frame) {
      Object leftVal = left.execute(frame);
      Object rightVal = right.execute(frame);

      // Check for division by zero
      if (rightVal instanceof Long && (Long) rightVal == 0L) {
        throw new RuntimeException("Division by zero");
      } else if (rightVal instanceof Double && (Double) rightVal == 0.0) {
        throw new RuntimeException("Division by zero");
      }

      if (leftVal instanceof Long && rightVal instanceof Long) {
        return (Long) leftVal % (Long) rightVal;
      } else if (leftVal instanceof Double && rightVal instanceof Double) {
        return (Double) leftVal % (Double) rightVal;
      } else if (leftVal instanceof Long && rightVal instanceof Double) {
        return (Long) leftVal % (Double) rightVal;
      } else if (leftVal instanceof Double && rightVal instanceof Long) {
        return (Double) leftVal % (Long) rightVal;
      }

      return 0L;
    }
  }
}
