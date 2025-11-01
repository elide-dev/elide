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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import elide.lang.php.PhpLanguage;
import elide.lang.php.runtime.PhpContext;
import elide.lang.php.runtime.PhpGlobalScope;

/** Root node for PHP program execution. This is the entry point for executing PHP code. */
public final class PhpRootNode extends RootNode {

  @Child private PhpStatementNode body;

  public PhpRootNode(PhpLanguage language, FrameDescriptor frameDescriptor, PhpStatementNode body) {
    super(language, frameDescriptor);
    this.body = body;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    MaterializedFrame materializedFrame = frame.materialize();
    return executeBody(materializedFrame);
  }

  @TruffleBoundary
  private Object executeBody(MaterializedFrame frame) {
    // Set the global frame so that functions can access global variables
    PhpGlobalScope globalScope = getGlobalScopeFromContext();
    globalScope.setGlobalFrame(frame);

    body.executeVoid(frame);
    return 0; // PHP scripts return 0 by default
  }

  @TruffleBoundary
  private PhpGlobalScope getGlobalScopeFromContext() {
    PhpContext context = PhpContext.get(this);
    return context.getGlobalScope();
  }

  @Override
  public String getName() {
    return "PHP";
  }
}
