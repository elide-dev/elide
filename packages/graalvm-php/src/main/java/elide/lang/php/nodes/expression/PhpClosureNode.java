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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.runtime.PhpClosure;

/**
 * Node that creates a PHP closure. Evaluates captured variable expressions and creates a PhpClosure
 * object.
 */
public final class PhpClosureNode extends PhpExpressionNode {

  private final CallTarget callTarget;
  private final String[] parameterNames;

  @Children
  private final PhpExpressionNode[]
      capturedExpressions; // Expressions to evaluate for captured variables

  public PhpClosureNode(
      CallTarget callTarget, String[] parameterNames, PhpExpressionNode[] capturedExpressions) {
    this.callTarget = callTarget;
    this.parameterNames = parameterNames;
    this.capturedExpressions = capturedExpressions;
  }

  @Override
  @ExplodeLoop
  public Object execute(VirtualFrame frame) {
    // Evaluate all captured variable expressions
    Object[] capturedValues = new Object[capturedExpressions.length];
    for (int i = 0; i < capturedExpressions.length; i++) {
      capturedValues[i] = capturedExpressions[i].execute(frame);
    }

    return new PhpClosure(callTarget, parameterNames, capturedValues);
  }
}
