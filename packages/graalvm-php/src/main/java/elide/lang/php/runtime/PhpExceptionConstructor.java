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
package elide.lang.php.runtime;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.PhpLanguage;
import elide.lang.php.nodes.PhpMethodRootNode;
import elide.lang.php.nodes.PhpStatementNode;

/**
 * Factory for creating the built-in Exception class constructor. Constructor signature:
 * __construct($message = "", $code = 0)
 */
public final class PhpExceptionConstructor {

  public static PhpMethodRootNode create(PhpLanguage language) {
    FrameDescriptor frameDescriptor = buildFrameDescriptor();
    return new PhpMethodRootNode(
        language,
        frameDescriptor,
        "Exception",
        "__construct",
        new String[] {"message", "code"},
        new int[] {1, 2}, // param slots
        0, // $this slot
        new ExceptionConstructorBody(),
        -1 // no variadic parameter
        );
  }

  private static FrameDescriptor buildFrameDescriptor() {
    FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
    builder.addSlot(FrameSlotKind.Illegal, "this", null); // slot 0
    builder.addSlot(FrameSlotKind.Illegal, "message", null); // slot 1
    builder.addSlot(FrameSlotKind.Illegal, "code", null); // slot 2
    return builder.build();
  }

  private static final class ExceptionConstructorBody extends PhpStatementNode {
    @Override
    public void executeVoid(VirtualFrame frame) {
      // Arguments: [0] = $this (PhpObject), [1] = $message (optional), [2] = $code (optional)
      Object[] arguments = frame.getArguments();

      if (arguments.length < 1) {
        throw new RuntimeException("Exception constructor requires $this parameter");
      }

      PhpObject thisObject = (PhpObject) arguments[0];

      // Get message parameter (default to empty string)
      String message = "";
      if (arguments.length > 1 && arguments[1] != null) {
        message = String.valueOf(arguments[1]);
      }

      // Get code parameter (default to 0)
      long code = 0L;
      if (arguments.length > 2 && arguments[2] != null) {
        if (arguments[2] instanceof Long) {
          code = (Long) arguments[2];
        } else if (arguments[2] instanceof Integer) {
          code = ((Integer) arguments[2]).longValue();
        } else if (arguments[2] instanceof Double) {
          code = ((Double) arguments[2]).longValue();
        }
      }

      // Set properties on the exception object
      thisObject.writePropertyInternal("message", message);
      thisObject.writePropertyInternal("code", code);
    }
  }
}
