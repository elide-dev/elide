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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import elide.lang.php.nodes.PhpStatementNode;
import elide.lang.php.runtime.PhpClass;
import elide.lang.php.runtime.PhpObject;
import elide.lang.php.runtime.PhpThrowableException;

/**
 * Node for try/catch/finally blocks in PHP.
 *
 * <p>Execution flow: 1. Execute try block 2. If exception thrown, check catch clauses in order 3.
 * Execute matching catch block (if any) 4. Always execute finally block (if present) 5. Re-throw
 * exception if no catch matched
 */
public final class PhpTryNode extends PhpStatementNode {

  @Child private PhpStatementNode tryBody;

  @Children private final PhpStatementNode[] catchBodies;

  private final PhpCatchClause[] catchClauses;

  @Child private PhpStatementNode finallyBody;

  public PhpTryNode(
      PhpStatementNode tryBody, PhpCatchClause[] catchClauses, PhpStatementNode finallyBody) {
    this.tryBody = tryBody;
    this.catchClauses = catchClauses;
    // Extract catch bodies for @Children management
    this.catchBodies = new PhpStatementNode[catchClauses.length];
    for (int i = 0; i < catchClauses.length; i++) {
      this.catchBodies[i] = catchClauses[i].getBody();
    }
    this.finallyBody = finallyBody;
  }

  @Override
  @ExplodeLoop
  public void executeVoid(VirtualFrame frame) {
    PhpThrowableException caughtException = null;
    boolean exceptionHandled = false;

    try {
      // Execute try block
      tryBody.executeVoid(frame);
    } catch (PhpThrowableException e) {
      caughtException = e;
      PhpObject exceptionObject = e.getExceptionObject();

      // Try to find a matching catch clause
      for (int i = 0; i < catchClauses.length; i++) {
        PhpCatchClause catchClause = catchClauses[i];
        if (catchClause.matches(exceptionObject)) {
          // Store exception in the catch variable
          frame.setObject(catchClause.getVariableSlot(), exceptionObject);

          // Execute catch block
          try {
            catchBodies[i].executeVoid(frame);
            exceptionHandled = true;
            break;
          } catch (PhpThrowableException newException) {
            // New exception thrown in catch block
            caughtException = newException;
            exceptionHandled = false;
            break;
          }
        }
      }
    } finally {
      // Always execute finally block if present
      if (finallyBody != null) {
        finallyBody.executeVoid(frame);
      }
    }

    // Re-throw exception if it wasn't handled
    if (caughtException != null && !exceptionHandled) {
      throw caughtException;
    }
  }

  /** Represents a catch clause in a try/catch block. */
  public static final class PhpCatchClause {
    private final String exceptionClassName;
    private final String variableName;
    private final int variableSlot;
    private final PhpStatementNode body;

    public PhpCatchClause(
        String exceptionClassName, String variableName, int variableSlot, PhpStatementNode body) {
      this.exceptionClassName = exceptionClassName;
      this.variableName = variableName;
      this.variableSlot = variableSlot;
      this.body = body;
    }

    public String getExceptionClassName() {
      return exceptionClassName;
    }

    public String getVariableName() {
      return variableName;
    }

    public int getVariableSlot() {
      return variableSlot;
    }

    public PhpStatementNode getBody() {
      return body;
    }

    /**
     * Check if the given exception object matches this catch clause. For now, we do simple class
     * name matching. TODO: Support inheritance (Exception subclasses)
     */
    public boolean matches(PhpObject exceptionObject) {
      PhpClass exceptionClass = exceptionObject.getPhpClass();

      // Check if class name matches
      if (exceptionClass.getName().equals(exceptionClassName)) {
        return true;
      }

      // Check parent classes (inheritance)
      PhpClass parentClass = exceptionClass.getParentClass();
      while (parentClass != null) {
        if (parentClass.getName().equals(exceptionClassName)) {
          return true;
        }
        parentClass = parentClass.getParentClass();
      }

      return false;
    }
  }
}
