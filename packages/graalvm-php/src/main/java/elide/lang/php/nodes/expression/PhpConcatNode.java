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
import elide.lang.php.runtime.PhpStringUtil;

/** Node for string concatenation in PHP (the . operator). */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpConcatNode extends PhpExpressionNode {

  @Child protected PhpExpressionNode left;
  @Child protected PhpExpressionNode right;

  protected PhpConcatNode(PhpExpressionNode left, PhpExpressionNode right) {
    this.left = left;
    this.right = right;
  }

  public static PhpConcatNode create(PhpExpressionNode left, PhpExpressionNode right) {
    return new PhpConcatNodeImpl(left, right);
  }

  static final class PhpConcatNodeImpl extends PhpConcatNode {
    PhpConcatNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
      super(left, right);
    }

    @Override
    public Object execute(VirtualFrame frame) {
      Object leftVal = left.execute(frame);
      Object rightVal = right.execute(frame);

      // Convert both values to strings using PHP conversion rules (including __toString)
      String leftStr = PhpStringUtil.convertToString(leftVal);
      String rightStr = PhpStringUtil.convertToString(rightVal);

      return leftStr + rightStr;
    }
  }
}
