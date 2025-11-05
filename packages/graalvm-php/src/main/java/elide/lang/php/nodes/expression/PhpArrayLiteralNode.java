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
package elide.lang.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.runtime.PhpArray;

/** Node for array literals in PHP. Supports both indexed and associative arrays. */
public final class PhpArrayLiteralNode extends PhpExpressionNode {

  @Children private final PhpExpressionNode[] keys;

  @Children private final PhpExpressionNode[] values;

  private final boolean isAssociative;

  public PhpArrayLiteralNode(PhpExpressionNode[] values) {
    this.keys = null;
    this.values = values;
    this.isAssociative = false;
  }

  public PhpArrayLiteralNode(PhpExpressionNode[] keys, PhpExpressionNode[] values) {
    this.keys = keys;
    this.values = values;
    this.isAssociative = true;
  }

  @Override
  @ExplodeLoop
  public Object execute(VirtualFrame frame) {
    PhpArray array = new PhpArray();

    if (isAssociative) {
      for (int i = 0; i < keys.length; i++) {
        Object key = keys[i].execute(frame);
        Object value = values[i].execute(frame);
        array.put(key, value);
      }
    } else {
      for (int i = 0; i < values.length; i++) {
        Object value = values[i].execute(frame);
        array.append(value);
      }
    }

    return array;
  }
}
