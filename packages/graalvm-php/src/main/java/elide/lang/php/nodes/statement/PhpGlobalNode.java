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

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.nodes.PhpStatementNode;
import elide.lang.php.runtime.PhpContext;
import elide.lang.php.runtime.PhpGlobalScope;
import elide.lang.php.runtime.PhpReference;

/**
 * Node for PHP global statement. Syntax: global $varname;
 *
 * <p>Makes a local variable an alias to a global variable by storing the same PhpReference in both
 * the local and global frame slots.
 */
public final class PhpGlobalNode extends PhpStatementNode {

  private final String variableName;
  private final int localSlot;

  public PhpGlobalNode(String variableName, int localSlot) {
    this.variableName = variableName;
    this.localSlot = localSlot;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    PhpContext context = PhpContext.get(this);
    PhpGlobalScope globalScope = context.getGlobalScope();

    // Get or create the global slot for this variable
    int globalSlot = globalScope.getOrCreateGlobalSlot(variableName);

    // Get the global frame
    MaterializedFrame globalFrame = globalScope.getGlobalFrame();

    if (globalFrame == null) {
      // No global frame yet - this shouldn't happen in normal execution
      // but we'll handle it gracefully by doing nothing
      return;
    }

    // Get the value from the global slot
    Object globalValue = globalFrame.getObject(globalSlot);

    // Create or reuse a PhpReference for the global variable
    PhpReference reference;
    if (globalValue instanceof PhpReference) {
      // Global variable is already a reference, reuse it
      reference = (PhpReference) globalValue;
    } else {
      // Wrap the global value in a reference
      reference = new PhpReference(globalValue);
      // Store the reference back in the global frame
      globalFrame.setObject(globalSlot, reference);
    }

    // Store the same reference in the local frame slot
    // This creates an alias: local and global variable share the same PhpReference
    frame.setObject(localSlot, reference);
  }
}
