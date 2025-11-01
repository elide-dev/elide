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

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import elide.lang.php.PhpLanguage;
import elide.lang.php.nodes.statement.PhpReturnNode;
import elide.lang.php.runtime.PhpArray;

/**
 * Root node for PHP closure execution. Similar to PhpFunctionRootNode but handles captured
 * variables.
 */
public final class PhpClosureRootNode extends RootNode {

  private final String[] parameterNames;
  private final int[] parameterSlots;
  private final int variadicParamIndex; // -1 if no variadic parameter
  private final int[] capturedSlots; // Slots for captured variables

  @Child private PhpStatementNode body;

  public PhpClosureRootNode(
      PhpLanguage language,
      FrameDescriptor frameDescriptor,
      String[] parameterNames,
      int[] parameterSlots,
      PhpStatementNode body,
      int variadicParamIndex,
      int[] capturedSlots) {
    super(language, frameDescriptor);
    this.parameterNames = parameterNames;
    this.parameterSlots = parameterSlots;
    this.body = body;
    this.variadicParamIndex = variadicParamIndex;
    this.capturedSlots = capturedSlots;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    // Arguments array layout:
    // If capturedSlots.length > 0: [capturedValue0, capturedValue1, ..., arg0, arg1, ...]
    // Otherwise: [arg0, arg1, ...]
    Object[] arguments = frame.getArguments();

    int capturedCount = capturedSlots.length;
    int argOffset = capturedCount; // Arguments start after captured values

    // Set captured variables in frame
    for (int i = 0; i < capturedCount; i++) {
      if (i < arguments.length) {
        frame.setObject(capturedSlots[i], arguments[i]);
      }
    }

    // Initialize parameters from arguments (after captured values)
    if (variadicParamIndex >= 0) {
      // Handle variadic parameter
      // Assign fixed parameters up to the variadic one
      for (int i = 0; i < variadicParamIndex && argOffset + i < arguments.length; i++) {
        frame.setObject(parameterSlots[i], arguments[argOffset + i]);
      }

      // Collect remaining arguments into an array for the variadic parameter
      PhpArray variadicArray = new PhpArray();
      for (int i = argOffset + variadicParamIndex; i < arguments.length; i++) {
        variadicArray.append(arguments[i]);
      }
      frame.setObject(parameterSlots[variadicParamIndex], variadicArray);
    } else {
      // No variadic parameter - assign normally
      for (int i = 0; i < parameterSlots.length && argOffset + i < arguments.length; i++) {
        frame.setObject(parameterSlots[i], arguments[argOffset + i]);
      }
    }

    try {
      body.executeVoid(frame);
      return null; // Closures without explicit return return null
    } catch (PhpReturnNode.PhpReturnException e) {
      return e.getResult();
    }
  }

  @Override
  public String getName() {
    return "{closure}";
  }
}
