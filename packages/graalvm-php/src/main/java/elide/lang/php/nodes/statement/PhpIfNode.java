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
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.nodes.PhpStatementNode;

/** Node for if/else control flow in PHP. */
public final class PhpIfNode extends PhpStatementNode {

  @Child private PhpExpressionNode condition;

  @Child private PhpStatementNode thenBranch;

  @Child private PhpStatementNode elseBranch;

  private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

  public PhpIfNode(
      PhpExpressionNode condition, PhpStatementNode thenBranch, PhpStatementNode elseBranch) {
    this.condition = condition;
    this.thenBranch = thenBranch;
    this.elseBranch = elseBranch;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    MaterializedFrame materializedFrame = frame.materialize();
    executeIf(materializedFrame);
  }

  @TruffleBoundary
  private void executeIf(MaterializedFrame frame) {
    boolean conditionValue;
    try {
      conditionValue = condition.executeBoolean(frame);
    } catch (UnexpectedResultException e) {
      // Convert result to boolean using PHP truthiness rules
      conditionValue = convertToBoolean(e.getResult());
    }

    if (conditionProfile.profile(conditionValue)) {
      thenBranch.executeVoid(frame);
    } else if (elseBranch != null) {
      elseBranch.executeVoid(frame);
    }
  }

  /**
   * Convert a PHP value to boolean using PHP truthiness rules. - null, false, 0, 0.0, "", "0" are
   * falsy - Everything else is truthy
   */
  private boolean convertToBoolean(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof Long) {
      return (Long) value != 0;
    }
    if (value instanceof Double) {
      return (Double) value != 0.0;
    }
    if (value instanceof String) {
      String str = (String) value;
      return !str.isEmpty() && !str.equals("0");
    }
    return true;
  }
}
