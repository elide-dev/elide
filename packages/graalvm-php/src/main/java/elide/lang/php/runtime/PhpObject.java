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
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.util.HashMap;
import java.util.Map;

/** Represents a PHP object instance. Stores the object's class reference and property values. */
@ExportLibrary(InteropLibrary.class)
public final class PhpObject implements TruffleObject {

  private final PhpClass phpClass;
  private final Map<String, Object> properties;

  public PhpObject(PhpClass phpClass) {
    this.phpClass = phpClass;
    this.properties = new HashMap<>();

    // Initialize properties with default values (including inherited properties)
    for (Map.Entry<String, PhpClass.PropertyMetadata> entry :
        phpClass.getAllProperties().entrySet()) {
      // Skip static properties (they're stored in the class, not the instance)
      if (!entry.getValue().isStatic()) {
        properties.put(entry.getKey(), entry.getValue().getDefaultValue());
      }
    }
  }

  public PhpClass getPhpClass() {
    return phpClass;
  }

  /** Read a property value (external access - must be public). */
  public Object readProperty(String propertyName) {
    return readProperty(propertyName, null);
  }

  /**
   * Read a property value with caller context for visibility checking.
   *
   * @param propertyName The property to read
   * @param callerClass The class from which the access is being made (null for external access)
   */
  public Object readProperty(String propertyName, PhpClass callerClass) {
    // Check if property exists in class definition
    if (!phpClass.hasProperty(propertyName)) {
      // Try __get magic method
      if (phpClass.hasMethod("__get")) {
        PhpClass.MethodMetadata method = phpClass.getMethod("__get");
        CallTarget callTarget = method.getCallTarget();
        if (callTarget != null) {
          return callTarget.call(this, propertyName);
        }
      }
      throw new RuntimeException(
          "Undefined property: " + phpClass.getName() + "::$" + propertyName);
    }

    // Check visibility
    PhpClass.PropertyMetadata metadata = phpClass.getProperty(propertyName);
    if (!isPropertyAccessible(metadata, callerClass)) {
      // Try __get magic method for inaccessible properties
      if (phpClass.hasMethod("__get")) {
        PhpClass.MethodMetadata method = phpClass.getMethod("__get");
        CallTarget callTarget = method.getCallTarget();
        if (callTarget != null) {
          return callTarget.call(this, propertyName);
        }
      }
      String visibilityName = metadata.getVisibility().toString().toLowerCase();
      throw new RuntimeException(
          "Cannot access "
              + visibilityName
              + " property: "
              + phpClass.getName()
              + "::$"
              + propertyName);
    }

    return properties.get(propertyName);
  }

  /** Write a property value (external access - must be public). */
  public void writeProperty(String propertyName, Object value) {
    writeProperty(propertyName, value, null);
  }

  /**
   * Write a property value with caller context for visibility checking.
   *
   * @param propertyName The property to write
   * @param value The value to write
   * @param callerClass The class from which the access is being made (null for external access)
   */
  public void writeProperty(String propertyName, Object value, PhpClass callerClass) {
    // Check if property exists in class definition
    if (!phpClass.hasProperty(propertyName)) {
      // Try __set magic method
      if (phpClass.hasMethod("__set")) {
        PhpClass.MethodMetadata method = phpClass.getMethod("__set");
        CallTarget callTarget = method.getCallTarget();
        if (callTarget != null) {
          callTarget.call(this, propertyName, value);
          return;
        }
      }
      throw new RuntimeException(
          "Undefined property: " + phpClass.getName() + "::$" + propertyName);
    }

    // Check visibility
    PhpClass.PropertyMetadata metadata = phpClass.getProperty(propertyName);
    if (!isPropertyAccessible(metadata, callerClass)) {
      // Try __set magic method for inaccessible properties
      if (phpClass.hasMethod("__set")) {
        PhpClass.MethodMetadata method = phpClass.getMethod("__set");
        CallTarget callTarget = method.getCallTarget();
        if (callTarget != null) {
          callTarget.call(this, propertyName, value);
          return;
        }
      }
      String visibilityName = metadata.getVisibility().toString().toLowerCase();
      throw new RuntimeException(
          "Cannot access "
              + visibilityName
              + " property: "
              + phpClass.getName()
              + "::$"
              + propertyName);
    }

    properties.put(propertyName, value);
  }

  /** Read property internally (from within methods, bypasses visibility). */
  public Object readPropertyInternal(String propertyName) {
    if (!phpClass.hasProperty(propertyName)) {
      throw new RuntimeException(
          "Undefined property: " + phpClass.getName() + "::$" + propertyName);
    }
    return properties.get(propertyName);
  }

  /** Write property internally (from within methods, bypasses visibility). */
  public void writePropertyInternal(String propertyName, Object value) {
    if (!phpClass.hasProperty(propertyName)) {
      throw new RuntimeException(
          "Undefined property: " + phpClass.getName() + "::$" + propertyName);
    }
    properties.put(propertyName, value);
  }

  /** Check if a property is accessible from the given caller class context. */
  private boolean isPropertyAccessible(PhpClass.PropertyMetadata metadata, PhpClass callerClass) {
    return isAccessible(
        metadata.getVisibility(), getPropertyDefiningClass(metadata.getName()), callerClass);
  }

  /** Get the class where a property is defined (walks inheritance chain). */
  private PhpClass getPropertyDefiningClass(String propertyName) {
    // Check if defined in this class
    if (phpClass.getProperties().containsKey(propertyName)) {
      return phpClass;
    }
    // Check parent classes
    PhpClass current = phpClass.getParentClass();
    while (current != null) {
      if (current.getProperties().containsKey(propertyName)) {
        return current;
      }
      current = current.getParentClass();
    }
    return phpClass; // Fallback to current class
  }

  /**
   * Check if a member with the given visibility is accessible from the caller class.
   *
   * @param visibility The visibility of the member
   * @param definingClass The class where the member is defined
   * @param callerClass The class from which the access is being made (null = external access)
   * @return true if accessible, false otherwise
   */
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

  @Override
  public String toString() {
    return phpClass.getName() + " Object";
  }

  // Interop messages for Truffle
  @ExportMessage
  boolean hasMembers() {
    return true;
  }

  @ExportMessage
  Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
    return properties.keySet().toArray(new String[0]);
  }

  @ExportMessage
  boolean isMemberReadable(String member) {
    return phpClass.hasProperty(member);
  }

  @ExportMessage
  Object readMember(String member) {
    return readProperty(member);
  }

  @ExportMessage
  boolean isMemberModifiable(String member) {
    return phpClass.hasProperty(member);
  }

  @ExportMessage
  boolean isMemberInsertable(@SuppressWarnings("unused") String member) {
    // PHP objects can only have properties defined in their class
    return false;
  }

  @ExportMessage
  void writeMember(String member, Object value) {
    writeProperty(member, value);
  }
}
