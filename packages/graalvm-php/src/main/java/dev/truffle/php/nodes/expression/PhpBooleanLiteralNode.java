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
package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import dev.truffle.php.nodes.PhpExpressionNode;

/** Node representing a boolean literal in PHP. PHP has two boolean values: true and false. */
public final class PhpBooleanLiteralNode extends PhpExpressionNode {

  private final boolean value;

  public PhpBooleanLiteralNode(boolean value) {
    this.value = value;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return value;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
    return value;
  }
}
