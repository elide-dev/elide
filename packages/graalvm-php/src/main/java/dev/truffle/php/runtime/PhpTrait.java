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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a PHP trait definition. Traits are a mechanism for code reuse in single-inheritance
 * languages like PHP. They allow horizontal composition of behavior without using inheritance.
 *
 * <p>Key characteristics:
 *
 * <ul>
 *   <li>Traits cannot be instantiated directly
 *   <li>Traits can contain methods and properties
 *   <li>Traits can use other traits (nested composition)
 *   <li>Traits can have abstract methods
 *   <li>Methods and properties are copied into classes at composition time
 * </ul>
 */
public final class PhpTrait {

  private final String name;
  private final Map<String, PhpClass.MethodMetadata> methods;
  private final Map<String, PhpClass.PropertyMetadata> properties;
  private final List<PhpTrait> usedTraits; // Traits used by this trait (nested composition)

  public PhpTrait(
      String name,
      Map<String, PhpClass.MethodMetadata> methods,
      Map<String, PhpClass.PropertyMetadata> properties) {
    this.name = name;
    this.methods = new HashMap<>(methods);
    this.properties = new HashMap<>(properties);
    this.usedTraits = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public Map<String, PhpClass.MethodMetadata> getMethods() {
    return methods;
  }

  public Map<String, PhpClass.PropertyMetadata> getProperties() {
    return properties;
  }

  public List<PhpTrait> getUsedTraits() {
    return usedTraits;
  }

  public void addUsedTrait(PhpTrait trait) {
    usedTraits.add(trait);
  }

  /**
   * Check if this trait has a specific method.
   *
   * @param methodName The method name to check
   * @return true if the method exists in this trait
   */
  public boolean hasMethod(String methodName) {
    return methods.containsKey(methodName);
  }

  /**
   * Get a method from this trait.
   *
   * @param methodName The method name
   * @return The method metadata, or null if not found
   */
  public PhpClass.MethodMetadata getMethod(String methodName) {
    return methods.get(methodName);
  }

  /**
   * Check if this trait has a specific property.
   *
   * @param propertyName The property name to check
   * @return true if the property exists in this trait
   */
  public boolean hasProperty(String propertyName) {
    return properties.containsKey(propertyName);
  }

  /**
   * Get a property from this trait.
   *
   * @param propertyName The property name
   * @return The property metadata, or null if not found
   */
  public PhpClass.PropertyMetadata getProperty(String propertyName) {
    return properties.get(propertyName);
  }

  /**
   * Get all methods including methods from nested traits. This flattens the trait hierarchy and
   * returns all methods that would be contributed by this trait and all traits it uses.
   *
   * <p>Note: This method does NOT handle conflict resolution - that should be done at the class
   * composition level. This simply collects all methods, with child trait methods overriding parent
   * trait methods.
   *
   * @return Map of all flattened methods
   */
  public Map<String, PhpClass.MethodMetadata> getAllMethods() {
    Map<String, PhpClass.MethodMetadata> allMethods = new HashMap<>();

    // First, add methods from all used traits (depth-first)
    for (PhpTrait usedTrait : usedTraits) {
      allMethods.putAll(usedTrait.getAllMethods());
    }

    // Then add/override with this trait's methods
    allMethods.putAll(methods);

    return allMethods;
  }

  /**
   * Get all properties including properties from nested traits. This flattens the trait hierarchy
   * and returns all properties that would be contributed by this trait and all traits it uses.
   *
   * <p>Note: Property conflicts are typically a fatal error in PHP, but that validation should be
   * done at the class composition level.
   *
   * @return Map of all flattened properties
   */
  public Map<String, PhpClass.PropertyMetadata> getAllProperties() {
    Map<String, PhpClass.PropertyMetadata> allProperties = new HashMap<>();

    // First, add properties from all used traits (depth-first)
    for (PhpTrait usedTrait : usedTraits) {
      allProperties.putAll(usedTrait.getAllProperties());
    }

    // Then add/override with this trait's properties
    allProperties.putAll(properties);

    return allProperties;
  }

  /**
   * Get all abstract methods in this trait (including from nested traits).
   *
   * @return List of abstract method names
   */
  public List<String> getAbstractMethods() {
    List<String> abstractMethods = new ArrayList<>();

    // Collect from nested traits first
    for (PhpTrait usedTrait : usedTraits) {
      abstractMethods.addAll(usedTrait.getAbstractMethods());
    }

    // Collect from this trait's methods
    for (Map.Entry<String, PhpClass.MethodMetadata> entry : methods.entrySet()) {
      if (entry.getValue().isAbstract()) {
        // Remove from list if previously added by nested trait (overridden)
        abstractMethods.remove(entry.getKey());
        // Add if still abstract
        if (entry.getValue().isAbstract()) {
          abstractMethods.add(entry.getKey());
        }
      } else {
        // Concrete method - remove from abstract list if present
        abstractMethods.remove(entry.getKey());
      }
    }

    return abstractMethods;
  }

  @Override
  public String toString() {
    return "Trait[" + name + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PhpTrait)) return false;
    PhpTrait other = (PhpTrait) obj;
    return name.equals(other.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
