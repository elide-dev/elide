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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * Represents a PHP closure (anonymous function). Closures can capture variables from their
 * enclosing scope.
 */
@ExportLibrary(InteropLibrary.class)
public final class PhpClosure implements TruffleObject {

  private final CallTarget callTarget;
  private final String[] parameterNames;
  private final int parameterCount;
  private final Object[] capturedValues; // Values captured via use clause

  public PhpClosure(CallTarget callTarget, String[] parameterNames, Object[] capturedValues) {
    this.callTarget = callTarget;
    this.parameterNames = parameterNames;
    this.parameterCount = parameterNames.length;
    this.capturedValues = capturedValues;
  }

  public CallTarget getCallTarget() {
    return callTarget;
  }

  public String[] getParameterNames() {
    return parameterNames;
  }

  public int getParameterCount() {
    return parameterCount;
  }

  public Object[] getCapturedValues() {
    return capturedValues;
  }

  @ExportMessage
  boolean isExecutable() {
    return true;
  }

  @ExportMessage
  Object execute(Object[] arguments) {
    return callTarget.call(arguments);
  }

  @Override
  @TruffleBoundary
  public String toString() {
    return "Closure";
  }
}
