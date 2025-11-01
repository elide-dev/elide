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
import elide.lang.php.runtime.PhpTypeHint;

/** Node that wraps a return statement to perform runtime type checking. */
public final class PhpReturnTypeCheckNode extends PhpStatementNode {

  @Child private PhpStatementNode originalReturn;
  private final PhpTypeHint typeHint;
  private final String functionName;
  private final String currentClassName;

  public PhpReturnTypeCheckNode(
      PhpStatementNode originalReturn,
      PhpTypeHint typeHint,
      String functionName,
      String currentClassName) {
    this.originalReturn = originalReturn;
    this.typeHint = typeHint;
    this.functionName = functionName;
    this.currentClassName = currentClassName;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    // This is a bit tricky - we need to intercept the return value
    // For now, we'll let the return execute and validate in PhpFunctionRootNode
    originalReturn.executeVoid(frame);
  }

  public PhpTypeHint getTypeHint() {
    return typeHint;
  }

  public String getFunctionName() {
    return functionName;
  }

  public String getCurrentClassName() {
    return currentClassName;
  }
}
