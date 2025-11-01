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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a PHP class definition. Stores metadata about the class including properties, methods,
 * and constructor.
 */
public final class PhpClass {

  private final String name;
  private final Map<String, PropertyMetadata> properties;
  private final Map<String, MethodMetadata> methods;
  private final Map<String, ConstantMetadata> constants; // Class constants
  private final CallTarget constructor;
  private final Map<String, Object> staticPropertyValues; // Storage for static property values
  private PhpClass parentClass; // Parent class for inheritance (nullable)
  private final List<PhpInterface> implementedInterfaces; // Interfaces implemented by this class
  private final List<PhpTrait> usedTraits; // Traits used by this class
  private final boolean isAbstract; // Whether this class is abstract

  public PhpClass(
      String name,
      Map<String, PropertyMetadata> properties,
      Map<String, MethodMetadata> methods,
      CallTarget constructor,
      boolean isAbstract) {
    this.name = name;
    this.properties = new HashMap<>(properties);
    this.methods = new HashMap<>(methods);
    this.constants = new HashMap<>();
    this.constructor = constructor;
    this.isAbstract = isAbstract;
    this.staticPropertyValues = new HashMap<>();
    this.parentClass = null;
    this.implementedInterfaces = new ArrayList<>();
    this.usedTraits = new ArrayList<>();

    // Initialize static properties with their default values
    for (Map.Entry<String, PropertyMetadata> entry : properties.entrySet()) {
      PropertyMetadata prop = entry.getValue();
      if (prop.isStatic()) {
        staticPropertyValues.put(entry.getKey(), prop.getDefaultValue());
      }
    }
  }

  public PhpClass getParentClass() {
    return parentClass;
  }

  public void setParentClass(PhpClass parentClass) {
    this.parentClass = parentClass;
  }

  public List<PhpInterface> getImplementedInterfaces() {
    return implementedInterfaces;
  }

  public void addImplementedInterface(PhpInterface interfaceToAdd) {
    implementedInterfaces.add(interfaceToAdd);
  }

  public List<PhpTrait> getUsedTraits() {
    return usedTraits;
  }

  public void addUsedTrait(PhpTrait trait) {
    usedTraits.add(trait);
  }

  /**
   * Compose traits into this class. This flattens all trait methods and properties into the class.
   * Must be called after all traits have been added.
   */
  public void composeTraits(List<TraitConflictResolution> conflictResolutions) {
    if (usedTraits.isEmpty()) {
      return;
    }

    // Build a map of trait methods for conflict resolution
    Map<String, Map<String, MethodMetadata>> traitMethodMap =
        new HashMap<>(); // traitName -> (methodName -> metadata)
    for (PhpTrait trait : usedTraits) {
      traitMethodMap.put(trait.getName(), trait.getAllMethods());
    }

    // Process insteadof rules first to determine which methods to exclude
    Map<String, String> methodExclusions =
        new HashMap<>(); // methodName -> excluded traits (comma-separated)
    if (conflictResolutions != null) {
      for (TraitConflictResolution resolution : conflictResolutions) {
        if (resolution.type == TraitConflictResolution.Type.INSTEADOF) {
          // Mark these methods from excluded traits as excluded
          for (String excludedTrait : resolution.excludedTraits) {
            String key = resolution.methodName + "@" + excludedTrait;
            methodExclusions.put(key, resolution.traitName);
          }
        }
      }
    }

    // Collect all methods from all traits (with proper precedence and conflict resolution)
    for (PhpTrait trait : usedTraits) {
      // Get all flattened methods from trait (includes nested traits)
      Map<String, MethodMetadata> traitMethods = trait.getAllMethods();

      // Add trait methods to class (only if not already defined in class)
      // Class methods have highest precedence
      for (Map.Entry<String, MethodMetadata> entry : traitMethods.entrySet()) {
        String methodName = entry.getKey();

        // Check if this method is excluded due to insteadof
        String exclusionKey = methodName + "@" + trait.getName();
        if (methodExclusions.containsKey(exclusionKey)) {
          continue; // Skip this method from this trait
        }

        if (!methods.containsKey(methodName)) {
          methods.put(methodName, entry.getValue());
        }
      }

      // Get all flattened properties from trait
      Map<String, PropertyMetadata> traitProperties = trait.getAllProperties();

      // Add trait properties to class (only if not already defined)
      for (Map.Entry<String, PropertyMetadata> entry : traitProperties.entrySet()) {
        String propName = entry.getKey();
        if (!properties.containsKey(propName)) {
          properties.put(propName, entry.getValue());
          // Initialize static property if needed
          PropertyMetadata prop = entry.getValue();
          if (prop.isStatic()) {
            staticPropertyValues.put(propName, prop.getDefaultValue());
          }
        }
      }
    }

    // Process aliasing rules (must be done after method composition)
    if (conflictResolutions != null) {
      for (TraitConflictResolution resolution : conflictResolutions) {
        if (resolution.type == TraitConflictResolution.Type.ALIAS) {
          // Find the method to alias
          MethodMetadata sourceMethod = null;

          if (resolution.traitName != null) {
            // Qualified alias: TraitName::method
            Map<String, MethodMetadata> traitMethods = traitMethodMap.get(resolution.traitName);
            if (traitMethods != null) {
              sourceMethod = traitMethods.get(resolution.methodName);
            }
          } else {
            // Unqualified alias: just method name
            // Search through all trait methods
            for (Map.Entry<String, Map<String, MethodMetadata>> traitEntry :
                traitMethodMap.entrySet()) {
              MethodMetadata method = traitEntry.getValue().get(resolution.methodName);
              if (method != null) {
                sourceMethod = method;
                break;
              }
            }
          }

          if (sourceMethod != null) {
            // Create aliased method with potentially different visibility
            Visibility newVisibility =
                resolution.aliasVisibility != null
                    ? resolution.aliasVisibility
                    : sourceMethod.getVisibility();

            MethodMetadata aliasedMethod =
                new MethodMetadata(
                    resolution.aliasName,
                    newVisibility,
                    sourceMethod.isStatic(),
                    sourceMethod.isAbstract(),
                    sourceMethod.getCallTarget(),
                    sourceMethod.getParameterNames());

            // Add the aliased method
            // If aliasName equals methodName, we're just changing visibility
            methods.put(resolution.aliasName, aliasedMethod);

            // If we're just changing visibility (same name), update the original too
            if (resolution.aliasName.equals(resolution.methodName)
                && resolution.aliasVisibility != null) {
              methods.put(resolution.methodName, aliasedMethod);
            }
          }
        }
      }
    }
  }

  /** Represents a trait conflict resolution rule (needed for composeTraits). */
  public static final class TraitConflictResolution {
    public enum Type {
      INSTEADOF,
      ALIAS
    }

    public final Type type;
    public final String traitName;
    public final String methodName;
    public final List<String> excludedTraits;
    public final String aliasName;
    public final Visibility aliasVisibility;

    public TraitConflictResolution(
        Type type,
        String traitName,
        String methodName,
        List<String> excludedTraits,
        String aliasName,
        Visibility aliasVisibility) {
      this.type = type;
      this.traitName = traitName;
      this.methodName = methodName;
      this.excludedTraits = excludedTraits;
      this.aliasName = aliasName;
      this.aliasVisibility = aliasVisibility;
    }
  }

  /**
   * Check if this class implements a specific interface (directly or via inheritance).
   *
   * @param interfaceName The name of the interface to check
   * @return true if this class implements the interface, false otherwise
   */
  public boolean implementsInterface(String interfaceName) {
    // Check direct implementations
    for (PhpInterface iface : implementedInterfaces) {
      if (iface.getName().equals(interfaceName)) {
        return true;
      }
      // Check parent interfaces recursively
      if (implementsInterfaceRecursive(iface, interfaceName)) {
        return true;
      }
    }
    // Check parent class
    if (parentClass != null) {
      return parentClass.implementsInterface(interfaceName);
    }
    return false;
  }

  /** Helper method to recursively check if an interface extends another interface. */
  private boolean implementsInterfaceRecursive(PhpInterface iface, String interfaceName) {
    for (PhpInterface parent : iface.getParentInterfaces()) {
      if (parent.getName().equals(interfaceName)) {
        return true;
      }
      if (implementsInterfaceRecursive(parent, interfaceName)) {
        return true;
      }
    }
    return false;
  }

  public String getName() {
    return name;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public Map<String, PropertyMetadata> getProperties() {
    return properties;
  }

  public Map<String, MethodMetadata> getMethods() {
    return methods;
  }

  public CallTarget getConstructor() {
    return constructor;
  }

  public boolean hasMethod(String methodName) {
    if (methods.containsKey(methodName)) {
      return true;
    }
    // Check parent class if present
    if (parentClass != null) {
      return parentClass.hasMethod(methodName);
    }
    return false;
  }

  public MethodMetadata getMethod(String methodName) {
    MethodMetadata method = methods.get(methodName);
    if (method != null) {
      return method;
    }
    // Check parent class if present
    if (parentClass != null) {
      return parentClass.getMethod(methodName);
    }
    return null;
  }

  public boolean hasProperty(String propertyName) {
    if (properties.containsKey(propertyName)) {
      return true;
    }
    // Check parent class if present
    if (parentClass != null) {
      return parentClass.hasProperty(propertyName);
    }
    return false;
  }

  public PropertyMetadata getProperty(String propertyName) {
    PropertyMetadata property = properties.get(propertyName);
    if (property != null) {
      return property;
    }
    // Check parent class if present
    if (parentClass != null) {
      return parentClass.getProperty(propertyName);
    }
    return null;
  }

  /**
   * Get all properties including inherited ones. Properties are returned in inheritance order:
   * parent first, then child. Child properties with same name override parent properties.
   */
  public Map<String, PropertyMetadata> getAllProperties() {
    Map<String, PropertyMetadata> allProps = new HashMap<>();

    // Start with parent properties (if parent exists)
    if (parentClass != null) {
      allProps.putAll(parentClass.getAllProperties());
    }

    // Add/override with this class's properties
    allProps.putAll(properties);

    return allProps;
  }

  /** Get the constructor for this class, or inherited from parent. */
  public CallTarget getConstructorOrInherited() {
    if (constructor != null) {
      return constructor;
    }
    // Check parent class if present
    if (parentClass != null) {
      return parentClass.getConstructorOrInherited();
    }
    return null;
  }

  public Object getStaticPropertyValue(String propertyName) {
    return staticPropertyValues.get(propertyName);
  }

  public void setStaticPropertyValue(String propertyName, Object value) {
    staticPropertyValues.put(propertyName, value);
  }

  public boolean hasStaticProperty(String propertyName) {
    PropertyMetadata prop = properties.get(propertyName);
    return prop != null && prop.isStatic();
  }

  /** Add a constant to this class. */
  public void addConstant(String name, Object value, Visibility visibility) {
    constants.put(name, new ConstantMetadata(name, value, visibility));
  }

  /** Check if this class has a constant (includes inherited constants). */
  public boolean hasConstant(String constantName) {
    if (constants.containsKey(constantName)) {
      return true;
    }
    // Check parent class if present
    if (parentClass != null) {
      return parentClass.hasConstant(constantName);
    }
    return false;
  }

  /** Get a constant value (includes inherited constants). */
  public Object getConstant(String constantName) {
    ConstantMetadata constant = constants.get(constantName);
    if (constant != null) {
      return constant.getValue();
    }
    // Check parent class if present
    if (parentClass != null) {
      return parentClass.getConstant(constantName);
    }
    return null;
  }

  /** Get constant metadata (includes inherited constants). */
  public ConstantMetadata getConstantMetadata(String constantName) {
    ConstantMetadata constant = constants.get(constantName);
    if (constant != null) {
      return constant;
    }
    // Check parent class if present
    if (parentClass != null) {
      return parentClass.getConstantMetadata(constantName);
    }
    return null;
  }

  /**
   * Check if a method is accessible from the given caller class context.
   *
   * @param methodName The method to check
   * @param callerClass The class from which the access is being made (null = external access)
   * @return true if accessible, false otherwise
   */
  public boolean isMethodAccessible(String methodName, PhpClass callerClass) {
    MethodMetadata method = getMethod(methodName);
    if (method == null) {
      return false;
    }

    PhpClass definingClass = getMethodDefiningClass(methodName);
    return isAccessible(method.getVisibility(), definingClass, callerClass);
  }

  /** Get the class where a method is defined (walks inheritance chain). */
  private PhpClass getMethodDefiningClass(String methodName) {
    // Check if defined in this class
    if (methods.containsKey(methodName)) {
      return this;
    }
    // Check parent classes
    PhpClass current = parentClass;
    while (current != null) {
      if (current.getMethods().containsKey(methodName)) {
        return current;
      }
      current = current.getParentClass();
    }
    return this; // Fallback to current class
  }

  /** Check if a member with the given visibility is accessible from the caller class. */
  private boolean isAccessible(
      Visibility visibility, PhpClass definingClass, PhpClass callerClass) {
    switch (visibility) {
      case PUBLIC:
        return true;
      case PROTECTED:
        // Protected is accessible from the same class or subclasses
        if (callerClass == null) {
          return false; // External access not allowed
        }
        return isSameOrSubclass(callerClass, definingClass);
      case PRIVATE:
        // Private is only accessible from the exact same class
        if (callerClass == null) {
          return false; // External access not allowed
        }
        return callerClass == definingClass;
      default:
        return false;
    }
  }

  /** Check if testClass is the same as or a subclass of baseClass. */
  private boolean isSameOrSubclass(PhpClass testClass, PhpClass baseClass) {
    PhpClass current = testClass;
    while (current != null) {
      if (current == baseClass) {
        return true;
      }
      current = current.getParentClass();
    }
    return false;
  }

  /**
   * Get all unimplemented abstract methods for this class. This includes abstract methods from this
   * class and all parent classes that don't have concrete implementations in this class or its
   * parents.
   *
   * @return List of unimplemented abstract method names
   */
  public List<String> getUnimplementedAbstractMethods() {
    List<String> unimplemented = new ArrayList<>();

    // Collect all abstract methods from this class and parent classes
    Map<String, MethodMetadata> allAbstractMethods = new HashMap<>();
    collectAbstractMethods(this, allAbstractMethods);

    // Check which ones are still abstract (not overridden with concrete implementation)
    for (Map.Entry<String, MethodMetadata> entry : allAbstractMethods.entrySet()) {
      String methodName = entry.getKey();
      MethodMetadata method = getMethod(methodName);

      if (method != null && method.isAbstract()) {
        unimplemented.add(methodName);
      }
    }

    return unimplemented;
  }

  /** Recursively collect abstract methods from this class and all parent classes. */
  private void collectAbstractMethods(
      PhpClass phpClass, Map<String, MethodMetadata> abstractMethods) {
    if (phpClass == null) {
      return;
    }

    // Add abstract methods from this class
    for (Map.Entry<String, MethodMetadata> entry : phpClass.getMethods().entrySet()) {
      if (entry.getValue().isAbstract()) {
        // Only add if not already overridden by a child class
        if (!abstractMethods.containsKey(entry.getKey())) {
          abstractMethods.put(entry.getKey(), entry.getValue());
        }
      } else {
        // Concrete method found - remove from abstract list (it's implemented)
        abstractMethods.remove(entry.getKey());
      }
    }

    // Recursively check parent class
    collectAbstractMethods(phpClass.getParentClass(), abstractMethods);
  }

  /**
   * Validate that a concrete class has implemented all abstract methods. Throws a RuntimeException
   * if there are unimplemented abstract methods.
   */
  public void validateAbstractMethodsImplemented() {
    // Only validate concrete classes
    if (isAbstract) {
      return;
    }

    List<String> unimplemented = getUnimplementedAbstractMethods();
    if (!unimplemented.isEmpty()) {
      throw new RuntimeException(
          "Class "
              + name
              + " must implement abstract methods: "
              + String.join(", ", unimplemented));
    }
  }

  /** Metadata about a class property. */
  public static final class PropertyMetadata {
    private final String name;
    private final Visibility visibility;
    private final boolean isStatic;
    private final Object defaultValue;

    public PropertyMetadata(
        String name, Visibility visibility, boolean isStatic, Object defaultValue) {
      this.name = name;
      this.visibility = visibility;
      this.isStatic = isStatic;
      this.defaultValue = defaultValue;
    }

    public String getName() {
      return name;
    }

    public Visibility getVisibility() {
      return visibility;
    }

    // Legacy method for backwards compatibility
    public boolean isPublic() {
      return visibility == Visibility.PUBLIC;
    }

    public boolean isStatic() {
      return isStatic;
    }

    public Object getDefaultValue() {
      return defaultValue;
    }
  }

  /** Metadata about a class method. */
  public static final class MethodMetadata {
    private final String name;
    private final Visibility visibility;
    private final boolean isStatic;
    private final boolean isAbstract;
    private final CallTarget callTarget;
    private final String[] parameterNames;

    public MethodMetadata(
        String name,
        Visibility visibility,
        boolean isStatic,
        boolean isAbstract,
        CallTarget callTarget,
        String[] parameterNames) {
      this.name = name;
      this.visibility = visibility;
      this.isStatic = isStatic;
      this.isAbstract = isAbstract;
      this.callTarget = callTarget;
      this.parameterNames = parameterNames;
    }

    public String getName() {
      return name;
    }

    public Visibility getVisibility() {
      return visibility;
    }

    // Legacy method for backwards compatibility
    public boolean isPublic() {
      return visibility == Visibility.PUBLIC;
    }

    public boolean isStatic() {
      return isStatic;
    }

    public boolean isAbstract() {
      return isAbstract;
    }

    public CallTarget getCallTarget() {
      return callTarget;
    }

    public String[] getParameterNames() {
      return parameterNames;
    }
  }

  /** Metadata about a class constant. */
  public static final class ConstantMetadata {
    private final String name;
    private final Object value;
    private final Visibility visibility;

    public ConstantMetadata(String name, Object value, Visibility visibility) {
      this.name = name;
      this.value = value;
      this.visibility = visibility;
    }

    public String getName() {
      return name;
    }

    public Object getValue() {
      return value;
    }

    public Visibility getVisibility() {
      return visibility;
    }
  }
}
