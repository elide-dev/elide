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
import dev.truffle.php.runtime.PhpFunction;

/** Node for function definitions in PHP. Registers the function in the context for later calls. */
public final class PhpFunctionNode extends PhpStatementNode {

  private final String functionName;
  private final PhpFunction function;

  public PhpFunctionNode(String functionName, PhpFunction function) {
    this.functionName = functionName;
    this.function = function;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    // Function definitions in PHP are registered at parse time, not runtime
    // This node is mainly for structural purposes
    // The actual registration happens in the parser
  }

  public String getFunctionName() {
    return functionName;
  }

  public PhpFunction getFunction() {
    return function;
  }
}
