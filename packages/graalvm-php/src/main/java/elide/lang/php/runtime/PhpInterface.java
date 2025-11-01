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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a PHP interface definition. Interfaces define method contracts that classes must
 * implement. All interface methods are implicitly public and abstract.
 */
public final class PhpInterface {

  private final String name;
  private final Map<String, MethodSignature> methods;
  private final List<PhpInterface> parentInterfaces; // Interfaces can extend other interfaces

  public PhpInterface(String name, Map<String, MethodSignature> methods) {
    this.name = name;
    this.methods = new HashMap<>(methods);
    this.parentInterfaces = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public Map<String, MethodSignature> getMethods() {
    return methods;
  }

  public List<PhpInterface> getParentInterfaces() {
    return parentInterfaces;
  }

  public void addParentInterface(PhpInterface parent) {
    parentInterfaces.add(parent);
  }

  /** Check if this interface has a method (including inherited from parent interfaces). */
  public boolean hasMethod(String methodName) {
    if (methods.containsKey(methodName)) {
      return true;
    }
    // Check parent interfaces
    for (PhpInterface parent : parentInterfaces) {
      if (parent.hasMethod(methodName)) {
        return true;
      }
    }
    return false;
  }

  /** Get a method signature (including inherited from parent interfaces). */
  public MethodSignature getMethod(String methodName) {
    MethodSignature method = methods.get(methodName);
    if (method != null) {
      return method;
    }
    // Check parent interfaces
    for (PhpInterface parent : parentInterfaces) {
      method = parent.getMethod(methodName);
      if (method != null) {
        return method;
      }
    }
    return null;
  }

  /** Get all methods including inherited ones from parent interfaces. */
  public Map<String, MethodSignature> getAllMethods() {
    Map<String, MethodSignature> allMethods = new HashMap<>();

    // Start with parent interface methods
    for (PhpInterface parent : parentInterfaces) {
      allMethods.putAll(parent.getAllMethods());
    }

    // Add/override with this interface's methods
    allMethods.putAll(methods);

    return allMethods;
  }

  /**
   * Represents a method signature in an interface. Just the name and parameter names - no
   * implementation.
   */
  public static final class MethodSignature {
    private final String name;
    private final String[] parameterNames;

    public MethodSignature(String name, String[] parameterNames) {
      this.name = name;
      this.parameterNames = parameterNames;
    }

    public String getName() {
      return name;
    }

    public String[] getParameterNames() {
      return parameterNames;
    }

    public int getParameterCount() {
      return parameterNames.length;
    }
  }
}
