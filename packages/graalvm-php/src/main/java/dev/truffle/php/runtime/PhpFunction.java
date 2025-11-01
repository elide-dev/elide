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
package dev.truffle.php.runtime;

import com.oracle.truffle.api.CallTarget;

/** Represents a PHP function. */
public final class PhpFunction {

  private final String name;
  private final CallTarget callTarget;
  private final int parameterCount;
  private final String[] parameterNames;
  private final boolean[] referenceParameters; // Track which parameters are by-reference

  public PhpFunction(
      String name, CallTarget callTarget, int parameterCount, String[] parameterNames) {
    this(name, callTarget, parameterCount, parameterNames, null);
  }

  public PhpFunction(
      String name,
      CallTarget callTarget,
      int parameterCount,
      String[] parameterNames,
      boolean[] referenceParameters) {
    this.name = name;
    this.callTarget = callTarget;
    this.parameterCount = parameterCount;
    this.parameterNames = parameterNames;
    this.referenceParameters = referenceParameters;
  }

  public String getName() {
    return name;
  }

  public CallTarget getCallTarget() {
    return callTarget;
  }

  public int getParameterCount() {
    return parameterCount;
  }

  public String[] getParameterNames() {
    return parameterNames;
  }

  public boolean[] getReferenceParameters() {
    return referenceParameters;
  }

  public boolean isReferenceParameter(int index) {
    return referenceParameters != null
        && index < referenceParameters.length
        && referenceParameters[index];
  }
}
