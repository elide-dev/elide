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
import elide.lang.php.runtime.PhpContext;
import elide.lang.php.runtime.PhpTypeHint;

/** Node that performs runtime type checking for function/method parameters. */
public final class PhpParameterTypeCheckNode extends PhpStatementNode {

  private final String parameterName;
  private final int parameterSlot;
  private final PhpTypeHint typeHint;
  private final String currentClassName;

  public PhpParameterTypeCheckNode(
      String parameterName, int parameterSlot, PhpTypeHint typeHint, String currentClassName) {
    this.parameterName = parameterName;
    this.parameterSlot = parameterSlot;
    this.typeHint = typeHint;
    this.currentClassName = currentClassName;
  }

  @Override
  public void executeVoid(VirtualFrame frame) {
    Object value = frame.getValue(parameterSlot);
    PhpContext context = PhpContext.get(this);

    if (!typeHint.matches(value, context, currentClassName)) {
      String actualType = getActualType(value);
      throw new RuntimeException(
          "TypeError: Argument #1 ($"
              + parameterName
              + ") must be of type "
              + typeHint.getDisplayName()
              + ", "
              + actualType
              + " given");
    }
  }

  private String getActualType(Object value) {
    if (value == null) return "null";
    if (value instanceof String) return "string";
    if (value instanceof Long) return "int";
    if (value instanceof Double) return "float";
    if (value instanceof Boolean) return "bool";
    if (value instanceof elide.lang.php.runtime.PhpArray) return "array";
    if (value instanceof elide.lang.php.runtime.PhpObject) {
      return ((elide.lang.php.runtime.PhpObject) value).getPhpClass().getName();
    }
    return "unknown";
  }
}
