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

import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.nodes.PhpStatementNode;
import elide.lang.php.runtime.PhpReference;

/**
 * Node for PHP reference assignment. Syntax: $b =& $a
 *
 * <p>Creates an alias so both variables reference the same value.
 */
public final class PhpReferenceAssignmentNode extends PhpStatementNode {

  private final String targetName;
  private final int targetSlot;
  private final String sourceName;
  private final int sourceSlot;

  public PhpReferenceAssignmentNode(
      String targetName, int targetSlot, String sourceName, int sourceSlot) {
    this.targetName = targetName;
    this.targetSlot = targetSlot;
    this.sourceName = sourceName;
    this.sourceSlot = sourceSlot;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
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
}
