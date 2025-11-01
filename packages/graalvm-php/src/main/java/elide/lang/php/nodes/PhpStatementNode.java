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
package elide.lang.php.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Base class for PHP statement nodes.
 *
 * <p>Statements don't return meaningful values in PHP's imperative context, so we provide a
 * specialized execute method that returns void.
 */
public abstract class PhpStatementNode extends PhpNode {

  /** Execute this statement. Statements typically have side effects but don't return values. */
  public abstract void executeVoid(VirtualFrame frame);

  @Override
  public final Object execute(VirtualFrame frame) {
    executeVoid(frame);
    return null;
  }
}
