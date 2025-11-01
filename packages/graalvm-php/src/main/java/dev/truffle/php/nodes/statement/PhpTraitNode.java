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
package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpTrait;

/**
 * AST node for trait definition statements. Registers the trait in the context during execution.
 *
 * <p>Traits define reusable code components that can be composed into classes. This node is created
 * during parsing and executes to register the trait in the PHP context, making it available for use
 * in class definitions.
 */
public final class PhpTraitNode extends PhpStatementNode {

  private final PhpTrait phpTrait;

  public PhpTraitNode(PhpTrait phpTrait) {
    this.phpTrait = phpTrait;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    // Register the trait in the context
    PhpContext context = PhpContext.get(this);
    context.registerTrait(phpTrait);
  }

  /** Get the trait associated with this node. Used by the parser during trait composition. */
  public PhpTrait getPhpTrait() {
    return phpTrait;
  }
}
