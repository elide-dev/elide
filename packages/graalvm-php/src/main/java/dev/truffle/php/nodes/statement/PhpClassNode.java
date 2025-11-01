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
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;

/**
 * AST node for class definition statements. Registers the class in the context during execution.
 */
public final class PhpClassNode extends PhpStatementNode {

  private final PhpClass phpClass;

  public PhpClassNode(PhpClass phpClass) {
    this.phpClass = phpClass;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    // Register the class in the context
    PhpContext context = PhpContext.get(this);
    context.registerClass(phpClass);
  }
}
