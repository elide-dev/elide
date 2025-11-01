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
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpObject;
import dev.truffle.php.runtime.PhpThrowableException;

/**
 * Node for throw statements in PHP. Evaluates the exception expression and throws a
 * PhpThrowableException that propagates through the call stack until caught by a try/catch block.
 */
public final class PhpThrowNode extends PhpStatementNode {

  @Child private PhpExpressionNode exceptionNode;

  public PhpThrowNode(PhpExpressionNode exceptionNode) {
    this.exceptionNode = exceptionNode;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    Object exception = exceptionNode.execute(frame);

    if (!(exception instanceof PhpObject)) {
      throw new RuntimeException(
          "Can only throw objects, got: " + exception.getClass().getSimpleName());
    }

    throw new PhpThrowableException((PhpObject) exception);
  }
}
