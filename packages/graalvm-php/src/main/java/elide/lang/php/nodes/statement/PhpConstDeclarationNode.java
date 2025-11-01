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
import elide.lang.php.runtime.PhpContext;

/**
 * Statement node for defining a constant using the `const` keyword. Syntax: const CONSTANT_NAME =
 * value;
 */
public final class PhpConstDeclarationNode extends PhpStatementNode {

  private final String constantName;
  @Child private PhpExpressionNode valueNode;

  public PhpConstDeclarationNode(String constantName, PhpExpressionNode valueNode) {
    this.constantName = constantName;
    this.valueNode = valueNode;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    MaterializedFrame materializedFrame = frame.materialize();
    executeConstDeclaration(materializedFrame);
  }

  @TruffleBoundary
  private void executeConstDeclaration(MaterializedFrame frame) {
    // Evaluate the value expression
    Object value = valueNode.execute(frame);

    // Define the constant in the context
    PhpContext context = PhpContext.get(this);
    context.defineConstant(constantName, value);
  }
}
