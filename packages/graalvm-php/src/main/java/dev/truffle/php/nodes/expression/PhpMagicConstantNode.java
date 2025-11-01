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

/** Node representing PHP magic constants like __FILE__, __DIR__, __LINE__, etc. */
public final class PhpMagicConstantNode extends PhpExpressionNode {

  public enum MagicConstantType {
    FILE, // __FILE__ - full path and filename
    DIR, // __DIR__ - directory of the file
    LINE, // __LINE__ - current line number
    CLASS_NAME, // __CLASS__ - current class name
    METHOD, // __METHOD__ - current class method name
    FUNCTION, // __FUNCTION__ - current function name
    NAMESPACE, // __NAMESPACE__ - current namespace
    TRAIT // __TRAIT__ - current trait name
  }

  private final MagicConstantType type;
  private final Object value;

  public PhpMagicConstantNode(MagicConstantType type, Object value) {
    this.type = type;
    this.value = value;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return value;
  }
}
