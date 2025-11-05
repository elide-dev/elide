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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.nodes.PhpStatementNode;
import elide.lang.php.runtime.PhpArray;
import elide.lang.php.runtime.PhpBreakException;
import elide.lang.php.runtime.PhpContinueException;
import elide.lang.php.runtime.PhpReference;
import java.util.List;

/**
 * Node for foreach loops in PHP. Syntax: foreach ($array as $value) { body } foreach ($array as
 * $key => $value) { body }
 */
public final class PhpForeachNode extends PhpStatementNode {

  @Child private LoopNode loopNode;

  public PhpForeachNode(
      PhpExpressionNode arrayExpr, int valueSlot, Integer keySlot, PhpStatementNode body) {
    this(arrayExpr, valueSlot, keySlot, body, false);
  }

  public PhpForeachNode(
      PhpExpressionNode arrayExpr,
      int valueSlot,
      Integer keySlot,
      PhpStatementNode body,
      boolean valueByReference) {
    this.loopNode =
        Truffle.getRuntime()
            .createLoopNode(
                new PhpForeachRepeatingNode(arrayExpr, valueSlot, keySlot, body, valueByReference));
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    loopNode.execute(frame);
  }

  private static final class PhpForeachRepeatingNode extends Node implements RepeatingNode {

    @Child private PhpExpressionNode arrayExpr;

    private final int valueSlot;
    private final Integer keySlot; // null if no key variable
    private final boolean valueByReference; // true if value should be a reference

    @Child private PhpStatementNode body;

    private List<Object> keys;
    private int currentIndex;
    private PhpArray array;

    PhpForeachRepeatingNode(
        PhpExpressionNode arrayExpr,
        int valueSlot,
        Integer keySlot,
        PhpStatementNode body,
        boolean valueByReference) {
      this.arrayExpr = arrayExpr;
      this.valueSlot = valueSlot;
      this.keySlot = keySlot;
      this.body = body;
      this.valueByReference = valueByReference;
      this.currentIndex = 0;
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
      MaterializedFrame materializedFrame = frame.materialize();
      return executeRepeatingBoundary(materializedFrame);
    }

    @TruffleBoundary
    private boolean executeRepeatingBoundary(MaterializedFrame frame) {
      // First iteration: evaluate array and get keys
      if (keys == null) {
        Object arrayObj = arrayExpr.execute(frame);
        if (!(arrayObj instanceof PhpArray)) {
          return false; // Not an array, exit loop
        }
        array = (PhpArray) arrayObj;
        keys = array.keys();
        currentIndex = 0;
      }

      // Check if we're done iterating
      if (currentIndex >= keys.size()) {
        return false;
      }

      // Get current key and value
      Object key = keys.get(currentIndex);
      Object value = array.get(key);

      // Set key variable if needed
      if (keySlot != null) {
        frame.setObject(keySlot, key);
      }

      // Set value variable
      if (valueByReference) {
        // For foreach with reference, we wrap the array element in a PhpReference
        // and store it back in the array. This way, modifications to the loop
        // variable will update the array element.
        PhpReference ref;
        if (value instanceof PhpReference) {
          ref = (PhpReference) value;
        } else {
          ref = new PhpReference(value);
          array.put(key, ref);
        }
        frame.setObject(valueSlot, ref);
      } else {
        frame.setObject(valueSlot, value);
      }

      // Execute body
      try {
        body.executeVoid(frame);
      } catch (PhpContinueException e) {
        // Continue to next iteration
        currentIndex++;
        return true;
      } catch (PhpBreakException e) {
        // Exit loop
        return false;
      }

      // Move to next element
      currentIndex++;
      return true;
    }
  }
}
