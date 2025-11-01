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
import dev.truffle.php.runtime.PhpReference;

/**
 * Node for PHP postfix decrement operator on properties. Syntax: $obj->property--
 *
 * <p>Returns the current value and decrements the property.
 */
public final class PhpPropertyPostDecrementNode extends PhpExpressionNode {

  @Child private PhpPropertyAccessNode propertyAccessNode;

  public PhpPropertyPostDecrementNode(PhpPropertyAccessNode propertyAccessNode) {
    this.propertyAccessNode = propertyAccessNode;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    // Read the current value
    Object currentValue = propertyAccessNode.execute(frame);

    // Unwrap if it's a PhpReference
    Object actualValue = currentValue;
    if (currentValue instanceof PhpReference) {
      actualValue = ((PhpReference) currentValue).getValue();
    }

    // Convert to number and decrement
    long currentNum;
    if (actualValue instanceof Long) {
      currentNum = (Long) actualValue;
    } else if (actualValue instanceof Double) {
      currentNum = ((Double) actualValue).longValue();
    } else if (actualValue instanceof String) {
      try {
        currentNum = Long.parseLong((String) actualValue);
      } catch (NumberFormatException e) {
        currentNum = 0;
      }
    } else {
      currentNum = 0;
    }

    long newValue = currentNum - 1;

    // Get the object and property name to write back
    PhpExpressionNode objectNode = propertyAccessNode.getObjectNode();
    String propertyName = propertyAccessNode.getPropertyName();
    String currentClassName = propertyAccessNode.getCurrentClassName();

    // Create a write node to update the property
    PhpExpressionNode writeNode =
        new PhpPropertyWriteNode(
            objectNode, propertyName, new PhpIntegerLiteralNode(newValue), currentClassName);

    // Execute the write
    writeNode.execute(frame);

    // Return the original value (postfix)
    return currentNum;
  }
}
