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
 * AST node for calling methods on the current class using self:: keyword.
 *
 * <p>This node is used when code inside a class method calls a method on the same class using the
 * self:: keyword. This is typically used for static methods, but can also be used to call instance
 * methods in a static context.
 */
public final class PhpSelfMethodCallNode extends PhpExpressionNode {

  @Children private final PhpExpressionNode[] argumentNodes;

  private final String methodName;
  private final String currentClassName; // The class from which self:: is being called

  public PhpSelfMethodCallNode(
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
      throw new RuntimeException("Cannot use self:: in undefined class: " + currentClassName);
    }

    // Check if the current class has the method
    if (!currentClass.hasMethod(methodName)) {
      throw new RuntimeException(
          "Call to undefined method: " + currentClassName + "::" + methodName + "()");
    }

    PhpClass.MethodMetadata method = currentClass.getMethod(methodName);

    // For self:: calls in instance context, we need $this from the frame
    // For static methods, the first argument will be null or handled differently
    Object[] args;

    if (method.isStatic()) {
      // Static method: no $this argument
      args = new Object[argumentNodes.length];
      for (int i = 0; i < argumentNodes.length; i++) {
        args[i] = argumentNodes[i].execute(frame);
      }
    } else {
      // Instance method: need $this from frame
      Object thisObject = frame.getArguments()[0]; // $this is the first argument
      if (!(thisObject instanceof PhpObject)) {
        throw new RuntimeException(
            "self:: can only be used in object context for non-static methods");
      }

      // Prepare arguments: $this is first argument, then method parameters
      args = new Object[argumentNodes.length + 1];
      args[0] = thisObject; // First argument is $this
      for (int i = 0; i < argumentNodes.length; i++) {
        args[i + 1] = argumentNodes[i].execute(frame);
      }
    }

    // Call the method
    CallTarget callTarget = method.getCallTarget();
    return callTarget.call(args);
  }
}
