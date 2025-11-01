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
package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.RootNode;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpFunctionRootNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.expression.PhpReadVariableNode;

/** Node for return statements in PHP. Throws a control flow exception to exit the function. */
public final class PhpReturnNode extends PhpStatementNode {

  @Child private PhpExpressionNode valueNode;

  public PhpReturnNode(PhpExpressionNode valueNode) {
    this.valueNode = valueNode;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    Object value = null;

    if (valueNode != null) {
      // Check if we're in a function that returns by reference
      RootNode rootNode = getRootNode();
      boolean returnsByReference =
          rootNode instanceof PhpFunctionRootNode
              && ((PhpFunctionRootNode) rootNode).returnsByReference();

      if (returnsByReference && valueNode instanceof PhpReadVariableNode) {
        // For return-by-reference, we need to return the PhpReference object itself
        // instead of the unwrapped value
        int slot = ((PhpReadVariableNode) valueNode).getSlot();
        value = frame.getObject(slot);
      } else {
        // Normal return - evaluate the expression (which unwraps references)
        value = valueNode.execute(frame);
      }
    }

    throw new PhpReturnException(value);
  }

  /** Exception used to implement return statement control flow. */
  public static final class PhpReturnException extends ControlFlowException {
    private final Object result;

    public PhpReturnException(Object result) {
      this.result = result;
    }

    public Object getResult() {
      return result;
    }
  }
}
