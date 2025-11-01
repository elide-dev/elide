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

/** Node for for loops in PHP. Syntax: for (init; condition; increment) { body } */
public final class PhpForNode extends PhpStatementNode {

  @Child private PhpStatementNode init;

  @Child private LoopNode loopNode;

  public PhpForNode(
      PhpStatementNode init,
      PhpExpressionNode condition,
      PhpExpressionNode increment,
      PhpStatementNode body) {
    this.init = init;
    this.loopNode =
        Truffle.getRuntime().createLoopNode(new PhpForRepeatingNode(condition, increment, body));
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    if (init != null) {
      init.executeVoid(frame);
    }
    loopNode.execute(frame);
  }

  private static final class PhpForRepeatingNode extends Node implements RepeatingNode {

    @Child private PhpExpressionNode condition;

    @Child private PhpExpressionNode increment;

    @Child private PhpStatementNode body;

    PhpForRepeatingNode(
        PhpExpressionNode condition, PhpExpressionNode increment, PhpStatementNode body) {
      this.condition = condition;
      this.increment = increment;
      this.body = body;
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
      MaterializedFrame materializedFrame = frame.materialize();
      return executeRepeatingBoundary(materializedFrame);
    }

    @TruffleBoundary
    private boolean executeRepeatingBoundary(MaterializedFrame frame) {
      if (condition != null && !evaluateConditionAsBoolean(frame)) {
        return false;
      }

      try {
        body.executeVoid(frame);
      } catch (PhpContinueException e) {
        // Continue to next iteration after running increment
        if (increment != null) {
          increment.execute(frame);
        }
        return true;
      } catch (PhpBreakException e) {
        // Exit loop
        return false;
      }

      if (increment != null) {
        increment.execute(frame);
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
