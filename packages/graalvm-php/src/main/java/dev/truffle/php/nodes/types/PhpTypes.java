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
package dev.truffle.php.nodes.types;

import com.oracle.truffle.api.dsl.TypeSystem;
import dev.truffle.php.runtime.PhpArray;

/**
 * PHP Type System for Truffle DSL.
 *
 * <p>Defines the basic types in PHP: - long (integer) - double (float) - boolean - String - null
 */
@TypeSystem({long.class, double.class, boolean.class, String.class})
public abstract class PhpTypes {

  /** Check if a value is null (PHP's null type). */
  public static boolean isNull(Object value) {
    return value == null;
  }

  /**
   * Convert a value to boolean using PHP's truthiness rules.
   *
   * <p>Falsy values in PHP: - null - false - 0 (integer) - 0.0 (float) - "" (empty string) - "0"
   * (string containing zero) - empty array
   *
   * <p>Everything else is truthy.
   */
  public static boolean toBoolean(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof Long) {
      return ((Long) value) != 0L;
    }
    if (value instanceof Double) {
      return ((Double) value) != 0.0;
    }
    if (value instanceof String) {
      String str = (String) value;
      return !str.isEmpty() && !str.equals("0");
    }
    if (value instanceof PhpArray) {
      return ((PhpArray) value).size() > 0;
    }
    // Objects and other types are truthy
    return true;
  }

  /**
   * Check if a value is empty using PHP's empty() rules.
   *
   * <p>Empty values in PHP: - null - false - 0 (integer) - 0.0 (float) - "" (empty string) - "0"
   * (string containing zero) - empty array
   *
   * <p>Same as !toBoolean(value)
   */
  public static boolean isEmpty(Object value) {
    return !toBoolean(value);
  }
}
