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

/** Node for logical NOT (!) in PHP. */
@NodeChild("operand")
public abstract class PhpLogicalNotNode extends PhpExpressionNode {

  @Child protected PhpExpressionNode operand;

  protected PhpLogicalNotNode(PhpExpressionNode operand) {
    this.operand = operand;
  }

  public static PhpLogicalNotNode create(PhpExpressionNode operand) {
    return new PhpLogicalNotNodeImpl(operand);
  }

  static final class PhpLogicalNotNodeImpl extends PhpLogicalNotNode {
    PhpLogicalNotNodeImpl(PhpExpressionNode operand) {
      super(operand);
    }

    @Override
    public Object execute(VirtualFrame frame) {
      Object value = operand.execute(frame);
      return !isTruthy(value);
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
