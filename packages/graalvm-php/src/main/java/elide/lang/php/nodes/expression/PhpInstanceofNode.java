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

import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.runtime.PhpClass;
import elide.lang.php.runtime.PhpObject;

/**
 * Node for the instanceof operator.
 *
 * <p>Syntax: $obj instanceof ClassName
 *
 * <p>Returns true if the object is an instance of the specified class, any of its parent classes,
 * or if the class implements the specified interface.
 */
public final class PhpInstanceofNode extends PhpExpressionNode {

  @Child private PhpExpressionNode objectNode;

  private final String className;

  public PhpInstanceofNode(PhpExpressionNode objectNode, String className) {
    this.objectNode = objectNode;
    this.className = className;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object value = objectNode.execute(frame);

    // If the value is not a PhpObject, instanceof returns false
    if (!(value instanceof PhpObject)) {
      return false;
    }

    PhpObject phpObject = (PhpObject) value;
    PhpClass objectClass = phpObject.getPhpClass();

    // Check if the object's class matches the target class name
    if (objectClass.getName().equals(className)) {
      return true;
    }

    // Check parent classes (inheritance chain)
    PhpClass currentClass = objectClass.getParentClass();
    while (currentClass != null) {
      if (currentClass.getName().equals(className)) {
        return true;
      }
      currentClass = currentClass.getParentClass();
    }

    // Check if the class implements the interface
    if (objectClass.implementsInterface(className)) {
      return true;
    }

    return false;
  }
}
