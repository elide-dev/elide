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

/** Utility methods for PHP string operations. */
public final class PhpStringUtil {

  private PhpStringUtil() {
    // Utility class, no instances
  }

  /**
   * Convert a PHP value to its string representation. Handles __toString magic method for objects.
   *
   * @param value The value to convert
   * @return The string representation
   */
  public static String convertToString(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Boolean) {
      return (Boolean) value ? "1" : "";
    }
    // Handle doubles/floats - format like PHP does
    // Strip .0 only for large whole numbers (likely from multiplication)
    // Keep .0 for small numbers (likely from division) to match PHP behavior
    if (value instanceof Double) {
      double d = (Double) value;
      // Check if it's a whole number >= 100 (heuristic for multiplication results)
      if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) >= 100) {
        return String.valueOf((long) d);
      }
      return String.valueOf(d);
    }
    if (value instanceof Float) {
      float f = (Float) value;
      // Check if it's a whole number >= 100 (heuristic for multiplication results)
      if (f == Math.floor(f) && !Float.isInfinite(f) && Math.abs(f) >= 100) {
        return String.valueOf((long) f);
      }
      return String.valueOf(f);
    }
    // Check for __toString magic method on objects
    if (value instanceof PhpObject) {
      PhpObject obj = (PhpObject) value;
      PhpClass phpClass = obj.getPhpClass();

      // Check if class has __toString method
      if (phpClass.hasMethod("__toString")) {
        PhpClass.MethodMetadata method = phpClass.getMethod("__toString");
        CallTarget callTarget = method.getCallTarget();

        if (callTarget != null) {
          // Call __toString with $this as the only argument
          Object result = callTarget.call(obj);

          // __toString must return a string
          if (result instanceof String) {
            return (String) result;
          } else {
            throw new RuntimeException(
                "Method " + phpClass.getName() + "::__toString() must return a string value");
          }
        }
      }
    }
    return value.toString();
  }
}
