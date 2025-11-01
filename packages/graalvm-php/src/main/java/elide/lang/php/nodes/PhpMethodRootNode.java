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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import elide.lang.php.PhpLanguage;
import elide.lang.php.nodes.statement.PhpReturnNode;
import elide.lang.php.runtime.PhpArray;
import elide.lang.php.runtime.PhpObject;

/**
 * Root node for PHP method execution. Methods are similar to functions but have access to $this.
 */
public final class PhpMethodRootNode extends RootNode {

  private final String className;
  private final String methodName;
  private final String[] parameterNames;
  private final int[] parameterSlots;
  private final int thisSlot; // Slot for $this variable
  private final int variadicParamIndex; // -1 if no variadic parameter

  @Child private PhpStatementNode body;

  public PhpMethodRootNode(
      PhpLanguage language,
      FrameDescriptor frameDescriptor,
      String className,
      String methodName,
      String[] parameterNames,
      int[] parameterSlots,
      int thisSlot,
      PhpStatementNode body,
      int variadicParamIndex) {
    super(language, frameDescriptor);
    this.className = className;
    this.methodName = methodName;
    this.parameterNames = parameterNames;
    this.parameterSlots = parameterSlots;
    this.thisSlot = thisSlot;
    this.body = body;
    this.variadicParamIndex = variadicParamIndex;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    // Arguments: [0] = $this, [1..n] = method parameters
    Object[] arguments = frame.getArguments();

    // First argument is $this
    if (arguments.length > 0 && arguments[0] instanceof PhpObject) {
      frame.setObject(thisSlot, arguments[0]);
    }

    // Initialize method parameters from remaining arguments
    if (variadicParamIndex >= 0) {
      // Handle variadic parameter
      // Assign fixed parameters up to the variadic one
      for (int i = 0; i < variadicParamIndex && i + 1 < arguments.length; i++) {
        frame.setObject(parameterSlots[i], arguments[i + 1]);
      }

      // Collect remaining arguments into an array for the variadic parameter
      PhpArray variadicArray = new PhpArray();
      for (int i = variadicParamIndex + 1; i < arguments.length; i++) {
        variadicArray.append(arguments[i]);
      }
      frame.setObject(parameterSlots[variadicParamIndex], variadicArray);
    } else {
      // No variadic parameter - assign normally
      for (int i = 0; i < parameterSlots.length && i + 1 < arguments.length; i++) {
        frame.setObject(parameterSlots[i], arguments[i + 1]);
      }
    }

    MaterializedFrame materializedFrame = frame.materialize();
    return executeBody(materializedFrame);
  }

  @TruffleBoundary
  private Object executeBody(MaterializedFrame frame) {
    try {
      body.executeVoid(frame);
      return null; // Methods without explicit return return null
    } catch (PhpReturnNode.PhpReturnException e) {
      return e.getResult();
    }
  }

  @Override
  public String getName() {
    return className + "::" + methodName;
  }

  public int getThisSlot() {
    return thisSlot;
  }
}
