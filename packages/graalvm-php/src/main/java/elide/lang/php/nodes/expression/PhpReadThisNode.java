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

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.nodes.PhpExpressionNode;

/** AST node for reading the $this variable in methods. */
@NodeField(name = "slot", type = int.class)
public abstract class PhpReadThisNode extends PhpExpressionNode {

  protected abstract int getSlot();

  @Specialization
  protected Object readThis(VirtualFrame frame) {
    return frame.getObject(getSlot());
  }

  public static PhpReadThisNode create(int slot) {
    return PhpReadThisNodeGen.create(slot);
  }
}
