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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpReference;

/**
 * Node for writing a value to a variable. In PHP, assignments are expressions that return the
 * assigned value. Automatically handles PhpReference objects for by-reference variables.
 */
@NodeChild("value")
@NodeField(name = "slot", type = int.class)
public abstract class PhpWriteVariableNode extends PhpExpressionNode {

  @Child protected PhpExpressionNode value;

  protected PhpWriteVariableNode(PhpExpressionNode value) {
    this.value = value;
  }

  protected abstract int getSlot();

  public static PhpWriteVariableNode create(PhpExpressionNode value, int slot) {
    return new PhpWriteVariableNodeImpl(value, slot);
  }

  static final class PhpWriteVariableNodeImpl extends PhpWriteVariableNode {
    private final int slot;

    PhpWriteVariableNodeImpl(PhpExpressionNode value, int slot) {
      super(value);
      this.slot = slot;
    }

    @Override
    protected int getSlot() {
      return slot;
    }

    @Override
    public Object execute(VirtualFrame frame) {
      Object val = value.execute(frame);

      // Check if the slot contains a reference
      try {
        Object existing = frame.getObject(slot);
        if (existing instanceof PhpReference) {
          // Update the reference's value instead of replacing it
          ((PhpReference) existing).setValue(val);
          return val;
        }
      } catch (FrameSlotTypeException e) {
        // Slot doesn't exist yet, will be created below
      }

      // Normal assignment
      frame.setObject(slot, val);
      return val;
    }
  }
}
