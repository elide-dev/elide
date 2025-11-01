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
package elide.lang.php.nodes.statement;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.nodes.PhpStatementNode;
import elide.lang.php.runtime.PhpBreakException;
import elide.lang.php.runtime.PhpContinueException;

/** Node for while loops in PHP. */
public final class PhpWhileNode extends PhpStatementNode {

  @Child private LoopNode loopNode;

  public PhpWhileNode(PhpExpressionNode condition, PhpStatementNode body) {
    this.loopNode = Truffle.getRuntime().createLoopNode(new PhpWhileRepeatingNode(condition, body));
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    loopNode.execute(frame);
  }

  private static final class PhpWhileRepeatingNode extends Node implements RepeatingNode {

    @Child private PhpExpressionNode condition;

    @Child private PhpStatementNode body;

    PhpWhileRepeatingNode(PhpExpressionNode condition, PhpStatementNode body) {
      this.condition = condition;
      this.body = body;
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
      MaterializedFrame materializedFrame = frame.materialize();
      return executeRepeatingBoundary(materializedFrame);
    }

    @TruffleBoundary
    private boolean executeRepeatingBoundary(MaterializedFrame frame) {
      if (!evaluateConditionAsBoolean(frame)) {
        return false;
      }

      try {
        body.executeVoid(frame);
      } catch (PhpContinueException e) {
        // Continue to next iteration
        return true;
      } catch (PhpBreakException e) {
        // Exit loop
        return false;
      }
      return true;
    }

    private boolean evaluateConditionAsBoolean(MaterializedFrame frame) {
      Object value = condition.execute(frame);
      return isTruthy(value);
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
