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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpObject;

/**
 * AST node for calling parent class methods (parent::method()).
 *
 * <p>This node is used when code inside a class method calls a method on the parent class using the
 * parent:: keyword.
 */
public final class PhpParentMethodCallNode extends PhpExpressionNode {

  @Children private final PhpExpressionNode[] argumentNodes;

  private final String methodName;
  private final String currentClassName; // The class from which parent:: is being called

  public PhpParentMethodCallNode(
      String methodName, PhpExpressionNode[] argumentNodes, String currentClassName) {
    this.methodName = methodName;
    this.argumentNodes = argumentNodes;
    this.currentClassName = currentClassName;
  }

  @Override
  @ExplodeLoop
  public Object execute(VirtualFrame frame) {
    PhpContext context = PhpContext.get(this);

    // Get the current class
    PhpClass currentClass = context.getClass(currentClassName);
    if (currentClass == null) {
      throw new RuntimeException("Cannot use parent:: in undefined class: " + currentClassName);
    }

    // Get the parent class
    PhpClass parentClass = currentClass.getParentClass();
    if (parentClass == null) {
      throw new RuntimeException(
          "Cannot access parent:: when class " + currentClassName + " has no parent");
    }

    // Check if the parent class has the method
    if (!parentClass.hasMethod(methodName)) {
      throw new RuntimeException(
          "Call to undefined method: " + parentClass.getName() + "::" + methodName + "()");
    }

    PhpClass.MethodMetadata method = parentClass.getMethod(methodName);

    // For parent:: calls, we need $this from the frame
    Object thisObject = frame.getArguments()[0]; // $this is the first argument
    if (!(thisObject instanceof PhpObject)) {
      throw new RuntimeException("parent:: can only be used in object context");
    }

    // Prepare arguments: $this is first argument, then method parameters
    Object[] args = new Object[argumentNodes.length + 1];
    args[0] = thisObject; // First argument is $this
    for (int i = 0; i < argumentNodes.length; i++) {
      args[i + 1] = argumentNodes[i].execute(frame);
    }

    // Call the parent method
    CallTarget callTarget = method.getCallTarget();
    return callTarget.call(args);
  }
}
