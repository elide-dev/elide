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
package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/** Node for calling built-in PHP functions. */
public final class PhpBuiltinCallNode extends PhpExpressionNode {

  private final String functionName;
  private final CallTarget callTarget;

  @Children private final PhpExpressionNode[] argumentNodes;

  public PhpBuiltinCallNode(
      String functionName, CallTarget callTarget, PhpExpressionNode[] argumentNodes) {
    this.functionName = functionName;
    this.callTarget = callTarget;
    this.argumentNodes = argumentNodes;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    // Evaluate all argument expressions
    Object[] arguments = new Object[argumentNodes.length];
    for (int i = 0; i < argumentNodes.length; i++) {
      arguments[i] = argumentNodes[i].execute(frame);
    }

    // Call the built-in function
    return callTarget.call(arguments);
  }
}
