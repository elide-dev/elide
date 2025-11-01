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
 * Node for post-decrement operation ($var--). Decrements the variable and returns the old value.
 * Automatically handles PhpReference objects for by-reference variables.
 */
public final class PhpPostDecrementNode extends PhpExpressionNode {

  private final int slot;

  private PhpPostDecrementNode(int slot) {
    this.slot = slot;
  }

  public static PhpPostDecrementNode create(int slot) {
    return new PhpPostDecrementNode(slot);
  }

  @Override
  public Object execute(VirtualFrame frame) {
    // Read current value (may be a PhpReference)
    Object slotValue = frame.getObject(slot);
    Object current;
    PhpReference reference = null;

    // Unwrap reference if present
    if (slotValue instanceof PhpReference) {
      reference = (PhpReference) slotValue;
      current = reference.getValue();
    } else {
      current = slotValue;
    }

    // Store old value to return
    Object oldValue;
    Object newValue;

    if (current instanceof Long) {
      oldValue = current;
      newValue = (Long) current - 1;
    } else if (current instanceof Double) {
      oldValue = current;
      newValue = (Double) current - 1.0;
    } else if (current == null) {
      oldValue = null;
      newValue = -1L;
    } else {
      // Fallback: treat as 0
      oldValue = 0L;
      newValue = -1L;
    }

    // Write back new value (update reference or slot)
    if (reference != null) {
      reference.setValue(newValue);
    } else {
      frame.setObject(slot, newValue);
    }

    // Return old value
    return oldValue;
  }
}
