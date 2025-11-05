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
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.runtime.PhpArray;
import elide.lang.php.runtime.PhpReference;

/** Node for array access in PHP (e.g., $arr[0] or $arr["key"]). */
public final class PhpArrayAccessNode extends PhpExpressionNode {

  @Child private PhpExpressionNode arrayNode;

  @Child private PhpExpressionNode indexNode;

  public PhpArrayAccessNode(PhpExpressionNode arrayNode, PhpExpressionNode indexNode) {
    this.arrayNode = arrayNode;
    this.indexNode = indexNode;
  }

  public PhpExpressionNode getArrayNode() {
    return arrayNode;
  }

  public PhpExpressionNode getIndexNode() {
    return indexNode;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object arrayObj = arrayNode.execute(frame);
    Object index = indexNode.execute(frame);

    if (!(arrayObj instanceof PhpArray)) {
      throw new RuntimeException("Cannot use [] on non-array");
    }

    PhpArray array = (PhpArray) arrayObj;
    Object value = array.get(index);

    // Unwrap PhpReference if present
    // This happens when array elements are stored as references
    // (e.g., after foreach with &$value)
    if (value instanceof PhpReference) {
      return ((PhpReference) value).getValue();
    }

    return value;
  }
}
