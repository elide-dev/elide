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
package elide.lang.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import elide.lang.php.nodes.PhpExpressionNode;
import elide.lang.php.runtime.PhpClass;
import elide.lang.php.runtime.PhpContext;

/**
 * Node for post-decrement operation on static properties (ClassName::$property--). Decrements the
 * static property and returns the old value.
 */
public final class PhpStaticPropertyPostDecrementNode extends PhpExpressionNode {

  private final String className;
  private final String propertyName;

  public PhpStaticPropertyPostDecrementNode(String className, String propertyName) {
    this.className = className;
    this.propertyName = propertyName;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    PhpContext context = PhpContext.get(this);
    PhpClass phpClass = context.getClass(className);

    // If class not found, check if it's a trait name
    // When traits use self::, we need to resolve to the actual class that uses the trait
    if (phpClass == null) {
      // Check if className is a trait
      if (context.getTrait(className) != null) {
        // It's a trait - we need to find the actual class
        // The class context is determined by which class's method is currently executing
        // For static methods called as MyClass::method(), we need to find MyClass

        // Strategy: Find a class that has this static property and uses this trait
        phpClass = findClassUsingTrait(context, className, propertyName);

        if (phpClass == null) {
          throw new RuntimeException(
              "Cannot resolve class for trait '"
                  + className
                  + "' with static property $"
                  + propertyName);
        }
      } else {
        throw new RuntimeException("Class not found: " + className);
      }
    }

    if (!phpClass.hasStaticProperty(propertyName)) {
      throw new RuntimeException(
          "Static property " + className + "::$" + propertyName + " does not exist");
    }

    // Read current value
    Object current = phpClass.getStaticPropertyValue(propertyName);

    // Store old value to return
    Object oldValue;
    Object newValue;

    if (current instanceof Long) {
      oldValue = current;
      newValue = (Long) current - 1;
    } else if (current instanceof Double) {
      oldValue = current;
      newValue = (Double) current - 1.0;
    } else if (current == null) {
      oldValue = null;
      newValue = -1L;
    } else {
      // Fallback: treat as 0
      oldValue = 0L;
      newValue = -1L;
    }

    // Write back new value
    phpClass.setStaticPropertyValue(propertyName, newValue);

    // Return old value
    return oldValue;
  }

  /**
   * Find a class that uses the given trait and has the given static property. This is needed for
   * resolving self:: in trait methods.
   */
  private PhpClass findClassUsingTrait(PhpContext context, String traitName, String propertyName) {
    // We need to access all registered classes to find one that uses this trait
    return context.findClassUsingTraitWithProperty(traitName, propertyName);
  }
}
