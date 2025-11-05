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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import elide.lang.php.PhpLanguage;

/**
 * Base class for built-in PHP function root nodes. Built-in functions are implemented in Java and
 * registered in the PhpBuiltinRegistry.
 */
public abstract class PhpBuiltinRootNode extends RootNode {

  private final String functionName;

  protected PhpBuiltinRootNode(PhpLanguage language, String functionName) {
    super(language);
    this.functionName = functionName;
  }

  public String getFunctionName() {
    return functionName;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    // Arguments are passed as frame arguments
    Object[] args = frame.getArguments();
    return executeBuiltinBoundary(args);
  }

  @TruffleBoundary
  private Object executeBuiltinBoundary(Object[] args) {
    return executeBuiltin(args);
  }

  /**
   * Execute the built-in function with the given arguments.
   *
   * @param args The arguments passed to the function
   * @return The result of the function
   */
  protected abstract Object executeBuiltin(Object[] args);
}
