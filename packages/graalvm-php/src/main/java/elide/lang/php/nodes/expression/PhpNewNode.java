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
import elide.lang.php.runtime.PhpClass;
import elide.lang.php.runtime.PhpContext;
import elide.lang.php.runtime.PhpObject;

/**
 * AST node for object instantiation with the 'new' keyword. Creates a new object instance and calls
 * the constructor if present.
 */
public final class PhpNewNode extends PhpExpressionNode {

  private final String className;

  @Children private final PhpExpressionNode[] constructorArgs;

  public PhpNewNode(String className, PhpExpressionNode[] constructorArgs) {
    this.className = className;
    this.constructorArgs = constructorArgs;
  }

  @Override
  @ExplodeLoop
  public Object execute(VirtualFrame frame) {
    PhpContext context = PhpContext.get(this);
    PhpClass phpClass = context.getClass(className);

    if (phpClass == null) {
      throw new RuntimeException("Class not found: " + className);
    }

    // Check if class is abstract
    if (phpClass.isAbstract()) {
      throw new RuntimeException("Cannot instantiate abstract class: " + className);
    }

    // Create new object instance
    PhpObject object = new PhpObject(phpClass);

    // Call constructor if present (including inherited constructor)
    CallTarget constructor = phpClass.getConstructorOrInherited();
    if (constructor != null) {
      // Evaluate constructor arguments
      Object[] args = new Object[constructorArgs.length + 1];
      args[0] = object; // First argument is $this
      for (int i = 0; i < constructorArgs.length; i++) {
        args[i + 1] = constructorArgs[i].execute(frame);
      }

      // Call constructor
      constructor.call(args);
    }

    return object;
  }
}
