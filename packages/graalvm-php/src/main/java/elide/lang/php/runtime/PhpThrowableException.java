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

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * Exception for PHP throw statement. Carries a PHP exception object and propagates through the call
 * stack until caught by a matching catch block.
 */
public final class PhpThrowableException extends ControlFlowException {

  private final PhpObject exceptionObject;

  public PhpThrowableException(PhpObject exceptionObject) {
    this.exceptionObject = exceptionObject;
  }

  public PhpObject getExceptionObject() {
    return exceptionObject;
  }
}
