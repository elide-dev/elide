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

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpReference;

/**
 * Node for reading a variable value. In PHP, variables are prefixed with $. Automatically unwraps
 * PhpReference objects for by-reference variables.
 */
@NodeField(name = "slot", type = int.class)
public abstract class PhpReadVariableNode extends PhpExpressionNode {

  public abstract int getSlot();

  public static PhpReadVariableNode create(int slot) {
    return new PhpReadVariableNodeImpl(slot);
  }

  static final class PhpReadVariableNodeImpl extends PhpReadVariableNode {
    private final int slot;

    PhpReadVariableNodeImpl(int slot) {
      this.slot = slot;
    }

    @Override
    public int getSlot() {
      return slot;
    }

    @Override
    public Object execute(VirtualFrame frame) {
      try {
        Object value = frame.getObject(slot);

        // Automatically unwrap references
        if (value instanceof PhpReference) {
          return ((PhpReference) value).getValue();
        }

        return value;
      } catch (FrameSlotTypeException e) {
        return null;
      }
    }
  }
}
