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

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpObject;

/** AST node for writing object properties ($obj->property = value). */
public final class PhpPropertyWriteNode extends PhpExpressionNode {

  @Child private PhpExpressionNode objectNode;

  @Child private PhpExpressionNode valueNode;

  private final String propertyName;
  private final String
      callerClassName; // The name of the class from which this access is being made (null for

  // external)

  // Constructor with caller class name
  public PhpPropertyWriteNode(
      PhpExpressionNode objectNode,
      String propertyName,
      PhpExpressionNode valueNode,
      String callerClassName) {
    this.objectNode = objectNode;
    this.propertyName = propertyName;
    this.valueNode = valueNode;
    this.callerClassName = callerClassName;
  }

  // Legacy constructor for backward compatibility
  public PhpPropertyWriteNode(
      PhpExpressionNode objectNode,
      String propertyName,
      PhpExpressionNode valueNode,
      boolean isInternal) {
    this.objectNode = objectNode;
    this.propertyName = propertyName;
    this.valueNode = valueNode;
    this.callerClassName = null; // Treated as external access
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object objectValue = objectNode.execute(frame);

    if (!(objectValue instanceof PhpObject)) {
      throw new RuntimeException("Trying to set property of non-object");
    }

    PhpObject object = (PhpObject) objectValue;
    Object value = valueNode.execute(frame);

    // Look up caller class if we have a class name
    PhpClass callerClass = null;
    if (callerClassName != null) {
      PhpContext context = PhpContext.get(this);
      callerClass = context.getClass(callerClassName);
    }

    // Use visibility checking with callerClass
    object.writeProperty(propertyName, value, callerClass);

    return value;
  }
}
