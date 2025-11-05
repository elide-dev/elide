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

/** AST node for reading static class properties. Syntax: ClassName::$propertyName */
public final class PhpStaticPropertyAccessNode extends PhpExpressionNode {

  private final String className;
  private final String propertyName;

  public PhpStaticPropertyAccessNode(String className, String propertyName) {
    this.className = className;
    this.propertyName = propertyName;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    PhpContext context = PhpContext.get(this);
    PhpClass phpClass = context.getClass(className);

    if (phpClass == null) {
      throw new RuntimeException("Class not found: " + className);
    }

    if (!phpClass.hasStaticProperty(propertyName)) {
      throw new RuntimeException(
          "Static property " + className + "::$" + propertyName + " does not exist");
    }

    return phpClass.getStaticPropertyValue(propertyName);
  }
}
