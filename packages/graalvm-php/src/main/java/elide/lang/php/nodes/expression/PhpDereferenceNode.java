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

import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.runtime.PhpReference;

/**
 * Node that unwraps a PhpReference to get the underlying value. If the value is not a reference,
 * returns it as-is.
 */
public final class PhpDereferenceNode extends PhpExpressionNode {

  @Child private PhpExpressionNode referenceNode;

  public PhpDereferenceNode(PhpExpressionNode referenceNode) {
    this.referenceNode = referenceNode;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object value = referenceNode.execute(frame);

    // If it's a reference, unwrap it
    if (value instanceof PhpReference) {
      return ((PhpReference) value).getValue();
    }

    // Otherwise return as-is
    return value;
  }
}
