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
import elide.lang.php.runtime.PhpStringUtil;
import java.io.PrintWriter;

/**
 * Node for the echo statement in PHP. Echo can output one or more expressions, separated by commas.
 */
public final class PhpEchoNode extends PhpStatementNode {

  @Children private final PhpExpressionNode[] expressions;

  public PhpEchoNode(PhpExpressionNode[] expressions) {
    this.expressions = expressions;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    MaterializedFrame materializedFrame = frame.materialize();
    executeEcho(materializedFrame);
  }

  @TruffleBoundary
  private void executeEcho(MaterializedFrame frame) {
    PrintWriter output = getOutput();

    for (PhpExpressionNode expr : expressions) {
      Object value = expr.execute(frame);
      printValue(output, value);
    }
    flushOutput(output);
  }

  @TruffleBoundary
  private PrintWriter getOutput() {
    PhpContext context = PhpContext.get(this);
    return context.getOutput();
  }

  @TruffleBoundary
  private void printValue(PrintWriter output, Object value) {
    String str = PhpStringUtil.convertToString(value);
    output.print(str);
  }

  @TruffleBoundary
  private void flushOutput(PrintWriter output) {
    output.flush();
  }
}
