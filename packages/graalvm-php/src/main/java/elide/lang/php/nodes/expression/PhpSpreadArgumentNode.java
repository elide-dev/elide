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

/**
 * Node representing a spread operator argument in function calls (...$expr). The execute method
 * returns the expression value, and the isSpread flag indicates this argument should be unpacked.
 */
public final class PhpSpreadArgumentNode extends PhpExpressionNode {

  @Child private PhpExpressionNode expression;

  public PhpSpreadArgumentNode(PhpExpressionNode expression) {
    this.expression = expression;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return expression.execute(frame);
  }

  public boolean isSpread() {
    return true;
  }

  public PhpExpressionNode getExpression() {
    return expression;
  }
}
