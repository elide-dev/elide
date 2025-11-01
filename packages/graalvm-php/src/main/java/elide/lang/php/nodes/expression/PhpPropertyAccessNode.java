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
import elide.lang.php.runtime.PhpObject;

/** AST node for reading object properties ($obj->property). */
public final class PhpPropertyAccessNode extends PhpExpressionNode {

  @Child private PhpExpressionNode objectNode;

  private final String propertyName;
  private final String
      callerClassName; // The name of the class from which this access is being made (null for

  // external)

  // Constructor with caller class name
  public PhpPropertyAccessNode(
      PhpExpressionNode objectNode, String propertyName, String callerClassName) {
    this.objectNode = objectNode;
    this.propertyName = propertyName;
    this.callerClassName = callerClassName;
  }

  // Legacy constructor for backward compatibility
  public PhpPropertyAccessNode(
      PhpExpressionNode objectNode, String propertyName, boolean isInternal) {
    this.objectNode = objectNode;
    this.propertyName = propertyName;
    this.callerClassName = null; // Treated as external access
  }

  public PhpExpressionNode getObjectNode() {
    return objectNode;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String getCurrentClassName() {
    return callerClassName;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object objectValue = objectNode.execute(frame);

    if (!(objectValue instanceof PhpObject)) {
      throw new RuntimeException("Trying to get property of non-object");
    }

    PhpObject object = (PhpObject) objectValue;

    // Look up caller class if we have a class name
    PhpClass callerClass = null;
    if (callerClassName != null) {
      PhpContext context = PhpContext.get(this);
      callerClass = context.getClass(callerClassName);
    }

    // Use visibility checking with callerClass
    return object.readProperty(propertyName, callerClass);
  }
}
