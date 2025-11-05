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

/**
 * Node for accessing a class constant. Syntax: ClassName::CONSTANT_NAME, self::CONSTANT_NAME,
 * parent::CONSTANT_NAME
 */
public final class PhpClassConstantAccessNode extends PhpExpressionNode {

  private final String className;
  private final String constantName;

  public PhpClassConstantAccessNode(String className, String constantName) {
    this.className = className;
    this.constantName = constantName;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    PhpContext context = PhpContext.get(this);

    // Get the class
    PhpClass phpClass = context.getClass(className);
    if (phpClass == null) {
      throw new RuntimeException("Class not found: " + className);
    }

    // Get the constant value
    Object value = phpClass.getConstant(constantName);
    if (value == null) {
      throw new RuntimeException("Undefined class constant: " + className + "::" + constantName);
    }

    return value;
  }
}
