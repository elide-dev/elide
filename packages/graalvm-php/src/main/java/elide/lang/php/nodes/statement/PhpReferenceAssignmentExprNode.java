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
package elide.lang.php.nodes.statement;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.nodes.PhpStatementNode;
import elide.lang.php.nodes.expression.PhpArrayAccessNode;
import elide.lang.php.nodes.expression.PhpFunctionCallNode;
import elide.lang.php.nodes.expression.PhpReadVariableNode;
import elide.lang.php.runtime.PhpArray;
import elide.lang.php.runtime.PhpReference;

/**
 * Node for PHP reference assignment with expressions. Syntax: $b =& $a or $ref =& $arr[1] or $ref
 * =& functionCall()
 *
 * <p>Creates an alias so the target variable references the same value as the source expression.
 * Supports: - Variable to variable: $b =& $a - Array element to variable: $ref =& $arr[1] -
 * Function call to variable: $ref =& getValue() (for functions that return by reference)
 */
public final class PhpReferenceAssignmentExprNode extends PhpStatementNode {

  private final String targetName;
  private final int targetSlot;

  @Child private PhpExpressionNode sourceExpr;

  public PhpReferenceAssignmentExprNode(
      String targetName, int targetSlot, PhpExpressionNode sourceExpr) {
    this.targetName = targetName;
    this.targetSlot = targetSlot;
    this.sourceExpr = sourceExpr;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    MaterializedFrame materializedFrame = frame.materialize();
    executeReferenceAssignment(materializedFrame);
  }

  @TruffleBoundary
  private void executeReferenceAssignment(MaterializedFrame frame) {
    // Check what type of expression we have
    if (sourceExpr instanceof PhpReadVariableNode) {
      // Variable-to-variable reference: $b =& $a
      handleVariableReference(frame, (PhpReadVariableNode) sourceExpr);
    } else if (sourceExpr instanceof PhpArrayAccessNode) {
      // Array element reference: $ref =& $arr[1]
      handleArrayElementReference(frame, (PhpArrayAccessNode) sourceExpr);
    } else if (sourceExpr instanceof PhpFunctionCallNode) {
      // Function call reference: $ref =& getValue()
      handleFunctionCallReference(frame, (PhpFunctionCallNode) sourceExpr);
    } else {
      throw new RuntimeException(
          "Only variables, array elements, and function calls can be assigned by reference");
    }
  }

  private void handleVariableReference(MaterializedFrame frame, PhpReadVariableNode varNode) {
    // Get the source variable slot
    int sourceSlot = varNode.getSlot();

    // Get the value from the source slot
    Object sourceValue = frame.getObject(sourceSlot);

    // If the source is already a PhpReference, use it
    // Otherwise, wrap the value and store it back in the source slot
    PhpReference reference;
    if (sourceValue instanceof PhpReference) {
      reference = (PhpReference) sourceValue;
    } else {
      // Create a new reference and update the source slot
      reference = new PhpReference(sourceValue);
      frame.setObject(sourceSlot, reference);
    }

    // Store the same reference in the target slot
    frame.setObject(targetSlot, reference);
  }

  private void handleArrayElementReference(MaterializedFrame frame, PhpArrayAccessNode arrayAccessNode) {
    // Get the array and index nodes
    PhpExpressionNode arrayNode = arrayAccessNode.getArrayNode();
    PhpExpressionNode indexNode = arrayAccessNode.getIndexNode();

    // Evaluate the array and index
    Object arrayObj = arrayNode.execute(frame);
    Object index = indexNode.execute(frame);

    if (!(arrayObj instanceof PhpArray)) {
      throw new RuntimeException("Cannot create reference to non-array element");
    }

    PhpArray array = (PhpArray) arrayObj;

    // Get the current value at this index
    Object currentValue = array.get(index);

    // If the array element is already a PhpReference, use it
    // Otherwise, create a new reference and store it in the array
    PhpReference reference;
    if (currentValue instanceof PhpReference) {
      reference = (PhpReference) currentValue;
    } else {
      // Wrap the value in a reference and update the array
      reference = new PhpReference(currentValue);
      array.put(index, reference);
    }

    // Store the same reference in the target variable
    frame.setObject(targetSlot, reference);
  }

  private void handleFunctionCallReference(
      MaterializedFrame frame, PhpFunctionCallNode functionCallNode) {
    // Execute the function call
    // If the function returns by reference, it will return a PhpReference object
    Object result = functionCallNode.execute(frame);

    // The function should return a PhpReference if it returns by reference
    if (result instanceof PhpReference) {
      // Store the reference in the target variable
      frame.setObject(targetSlot, result);
    } else {
      throw new RuntimeException(
          "Only functions that return by reference can be assigned by reference");
    }
  }
}
