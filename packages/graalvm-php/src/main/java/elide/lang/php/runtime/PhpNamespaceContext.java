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

import java.util.HashMap;
import java.util.Map;

/**
 * Context for PHP namespace resolution.
 *
 * <p>Manages the current namespace and use statements (imports) to properly resolve class,
 * function, and constant names according to PHP namespace rules.
 *
 * <p>PHP Namespace Rules:
 *
 * <ul>
 *   <li>Fully qualified names start with \ (e.g., \MyApp\Models\User)
 *   <li>Qualified names contain \ but don't start with it (e.g., Models\User)
 *   <li>Unqualified names don't contain \ (e.g., User)
 * </ul>
 */
public final class PhpNamespaceContext {

  private String currentNamespace;
  private final Map<String, String> classAliases; // alias -> fullyQualifiedName
  private final Map<String, String> functionAliases; // alias -> fullyQualifiedName
  private final Map<String, String> constAliases; // alias -> fullyQualifiedName

  public PhpNamespaceContext() {
    this.currentNamespace = ""; // Global namespace by default
    this.classAliases = new HashMap<>();
    this.functionAliases = new HashMap<>();
    this.constAliases = new HashMap<>();
  }

  /**
   * Set the current namespace.
   *
   * @param namespace The namespace name (e.g., "MyApp\\Controllers")
   */
  public void setCurrentNamespace(String namespace) {
    this.currentNamespace = namespace == null ? "" : namespace;
  }

  /**
   * Get the current namespace.
   *
   * @return The current namespace, or empty string for global namespace
   */
  public String getCurrentNamespace() {
    return currentNamespace;
  }

  /**
   * Add a class use statement (import).
   *
   * @param fullyQualifiedName The fully qualified class name
   * @param alias The alias to use (defaults to simple name if null)
   */
  public void addClassUse(String fullyQualifiedName, String alias) {
    if (alias == null) {
      // Extract simple name from fully qualified name
      alias = getSimpleName(fullyQualifiedName);
    }
    classAliases.put(alias, fullyQualifiedName);
  }

  /**
   * Add a function use statement (import).
   *
   * @param fullyQualifiedName The fully qualified function name
   * @param alias The alias to use (defaults to simple name if null)
   */
  public void addFunctionUse(String fullyQualifiedName, String alias) {
    if (alias == null) {
      alias = getSimpleName(fullyQualifiedName);
    }
    functionAliases.put(alias, fullyQualifiedName);
  }

  /**
   * Add a constant use statement (import).
   *
   * @param fullyQualifiedName The fully qualified constant name
   * @param alias The alias to use (defaults to simple name if null)
   */
  public void addConstUse(String fullyQualifiedName, String alias) {
    if (alias == null) {
      alias = getSimpleName(fullyQualifiedName);
    }
    constAliases.put(alias, fullyQualifiedName);
  }

  /**
   * Resolve a class name to its fully qualified name.
   *
   * @param name The class name to resolve
   * @return The fully qualified class name
   */
  public String resolveClassName(String name) {
    return resolveName(name, classAliases);
  }

  /**
   * Resolve a function name to its fully qualified name.
   *
   * @param name The function name to resolve
   * @return The fully qualified function name
   */
  public String resolveFunctionName(String name) {
    return resolveName(name, functionAliases);
  }

  /**
   * Resolve a constant name to its fully qualified name.
   *
   * @param name The constant name to resolve
   * @return The fully qualified constant name
   */
  public String resolveConstantName(String name) {
    return resolveName(name, constAliases);
  }

  /**
   * Resolve a name according to PHP namespace rules.
   *
   * @param name The name to resolve
   * @param aliases The alias map to check
   * @return The fully qualified name
   */
  private String resolveName(String name, Map<String, String> aliases) {
    // Rule 1: Fully qualified names (start with \) are used as-is
    if (name.startsWith("\\")) {
      return name.substring(1); // Remove leading backslash
    }

    // Rule 2: Check if it's an alias from a use statement
    if (aliases.containsKey(name)) {
      return aliases.get(name);
    }

    // Rule 3: Qualified names (contain \) are relative to current namespace
    if (name.contains("\\")) {
      if (currentNamespace.isEmpty()) {
        return name;
      }
      return currentNamespace + "\\" + name;
    }

    // Rule 4: Unqualified names
    // For classes: check aliases first (done in Rule 2), then prepend current namespace
    // For functions/constants: also fall back to global namespace if not found
    if (currentNamespace.isEmpty()) {
      return name; // Already in global namespace
    }
    return currentNamespace + "\\" + name;
  }

  /**
   * Extract the simple name from a fully qualified name. Example: "MyApp\\Models\\User" -> "User"
   *
   * @param fullyQualifiedName The fully qualified name
   * @return The simple name
   */
  private String getSimpleName(String fullyQualifiedName) {
    int lastSeparator = fullyQualifiedName.lastIndexOf('\\');
    if (lastSeparator >= 0) {
      return fullyQualifiedName.substring(lastSeparator + 1);
    }
    return fullyQualifiedName;
  }

  /** Clear all imports (use statements) but keep current namespace. */
  public void clearImports() {
    classAliases.clear();
    functionAliases.clear();
    constAliases.clear();
  }

  /** Reset to global namespace and clear all imports. */
  public void reset() {
    currentNamespace = "";
    clearImports();
  }

  /**
   * Create a copy of this namespace context.
   *
   * @return A new PhpNamespaceContext with the same state
   */
  public PhpNamespaceContext copy() {
    PhpNamespaceContext copy = new PhpNamespaceContext();
    copy.currentNamespace = this.currentNamespace;
    copy.classAliases.putAll(this.classAliases);
    copy.functionAliases.putAll(this.functionAliases);
    copy.constAliases.putAll(this.constAliases);
    return copy;
  }
}
