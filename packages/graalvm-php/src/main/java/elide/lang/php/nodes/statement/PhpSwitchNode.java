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
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.nodes.PhpStatementNode;
import elide.lang.php.runtime.PhpBreakException;

/**
 * AST node for switch statement. Evaluates an expression and executes code based on matching case
 * values. Supports fall-through behavior and default case.
 */
public final class PhpSwitchNode extends PhpStatementNode {

  @Child private PhpExpressionNode switchExpression;

  @Children private final PhpExpressionNode[] caseValues;

  @Children private final PhpStatementNode[] allStatements;

  private final int[] caseStartIndices; // Where each case's statements start
  private final int[] caseLengths; // How many statements in each case
  private final boolean[] isDefaultCase; // Whether each case is a default
  private final int defaultCaseIndex; // -1 if no default case

  public PhpSwitchNode(
      PhpExpressionNode switchExpression, CaseClause[] cases, int defaultCaseIndex) {
    this.switchExpression = switchExpression;
    this.defaultCaseIndex = defaultCaseIndex;

    // Flatten the structure for Truffle
    this.caseValues = new PhpExpressionNode[cases.length];
    this.caseStartIndices = new int[cases.length];
    this.caseLengths = new int[cases.length];
    this.isDefaultCase = new boolean[cases.length];

    int totalStatements = 0;
    for (CaseClause c : cases) {
      totalStatements += c.statements.length;
    }

    this.allStatements = new PhpStatementNode[totalStatements];

    int statementIndex = 0;
    for (int i = 0; i < cases.length; i++) {
      CaseClause c = cases[i];
      this.caseValues[i] = c.valueNode;
      this.isDefaultCase[i] = c.isDefault;
      this.caseStartIndices[i] = statementIndex;
      this.caseLengths[i] = c.statements.length;

      for (int j = 0; j < c.statements.length; j++) {
        this.allStatements[statementIndex++] = c.statements[j];
      }
    }
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    MaterializedFrame materializedFrame = frame.materialize();
    executeSwitchLogic(materializedFrame);
  }

  @TruffleBoundary
  private void executeSwitchLogic(MaterializedFrame frame) {
    Object switchValue = switchExpression.execute(frame);

    // Find matching case
    int matchingCaseIndex = -1;

    // Check all non-default cases for a match
    for (int i = 0; i < caseValues.length; i++) {
      if (isDefaultCase[i]) {
        continue; // Skip default case in this loop
      }

      Object caseValue = caseValues[i].execute(frame);
      if (phpEquals(switchValue, caseValue)) {
        matchingCaseIndex = i;
        break;
      }
    }

    // If no match found, use default case if present
    if (matchingCaseIndex == -1 && defaultCaseIndex != -1) {
      matchingCaseIndex = defaultCaseIndex;
    }

    // Execute from matching case onwards (fall-through behavior)
    if (matchingCaseIndex != -1) {
      try {
        int startStmt = caseStartIndices[matchingCaseIndex];
        int endStmt = allStatements.length;

        for (int i = startStmt; i < endStmt; i++) {
          allStatements[i].executeVoid(frame);
        }
      } catch (PhpBreakException e) {
        int level = e.getLevel();
        if (level > 1) {
          // Propagate to outer loop with decremented level
          throw new PhpBreakException(level - 1);
        }
        // Level 1 break - exit switch
      }
    }
  }

  /** PHP loose equality comparison for switch cases. */
  @TruffleBoundary
  private boolean phpEquals(Object a, Object b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    // For now, use Java's equals for simplicity
    // A full implementation would need type coercion rules
    if (a instanceof Long && b instanceof Long) {
      return ((Long) a).equals((Long) b);
    }
    if (a instanceof Double && b instanceof Double) {
      return ((Double) a).equals((Double) b);
    }
    if (a instanceof String && b instanceof String) {
      return ((String) a).equals((String) b);
    }
    if (a instanceof Boolean && b instanceof Boolean) {
      return ((Boolean) a).equals((Boolean) b);
    }
    // Mixed types - attempt numeric comparison
    if (a instanceof Number && b instanceof Number) {
      return ((Number) a).doubleValue() == ((Number) b).doubleValue();
    }
    return objectEquals(a, b);
  }

  @TruffleBoundary
  private static boolean objectEquals(Object a, Object b) {
    return a.equals(b);
  }

  /**
   * Represents a single case clause in a switch statement. This is just a data holder, not a Node.
   */
  public static final class CaseClause {
    public final PhpExpressionNode valueNode; // null for default case
    public final PhpStatementNode[] statements;
    public final boolean isDefault;

    public CaseClause(
        PhpExpressionNode valueNode, PhpStatementNode[] statements, boolean isDefault) {
      this.valueNode = valueNode;
      this.statements = statements;
      this.isDefault = isDefault;
    }
  }
}
