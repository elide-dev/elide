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
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpArray;

/** Node for writing to an array element (e.g., $arr[0] = value). */
public final class PhpArrayWriteNode extends PhpExpressionNode {

  @Child private PhpExpressionNode arrayNode;

  @Child private PhpExpressionNode indexNode;

  @Child private PhpExpressionNode valueNode;

  public PhpArrayWriteNode(
      PhpExpressionNode arrayNode, PhpExpressionNode indexNode, PhpExpressionNode valueNode) {
    this.arrayNode = arrayNode;
    this.indexNode = indexNode;
    this.valueNode = valueNode;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object arrayObj = arrayNode.execute(frame);
    Object index = indexNode != null ? indexNode.execute(frame) : null;
    Object value = valueNode.execute(frame);

    if (!(arrayObj instanceof PhpArray)) {
      throw new RuntimeException("Cannot use [] on non-array");
    }

    PhpArray array = (PhpArray) arrayObj;
    if (index == null) {
      // $arr[] = value (append)
      array.append(value);
    } else {
      array.put(index, value);
    }

    return value;
  }
}
