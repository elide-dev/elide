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
import elide.lang.php.runtime.PhpContext;

/** Node for accessing a parent class constant. Syntax: parent::CONSTANT_NAME */
public final class PhpParentConstantAccessNode extends PhpExpressionNode {

  private final String constantName;
  private final String currentClassName;

  public PhpParentConstantAccessNode(String constantName, String currentClassName) {
    this.constantName = constantName;
    this.currentClassName = currentClassName;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    PhpContext context = PhpContext.get(this);

    // Get the current class
    PhpClass currentClass = context.getClass(currentClassName);
    if (currentClass == null) {
      throw new RuntimeException("Class not found: " + currentClassName);
    }

    // Get the parent class
    PhpClass parentClass = currentClass.getParentClass();
    if (parentClass == null) {
      throw new RuntimeException("Class " + currentClassName + " has no parent class");
    }

    // Get the constant value from parent
    Object value = parentClass.getConstant(constantName);
    if (value == null) {
      throw new RuntimeException(
          "Undefined class constant: " + parentClass.getName() + "::" + constantName);
    }

    return value;
  }
}
