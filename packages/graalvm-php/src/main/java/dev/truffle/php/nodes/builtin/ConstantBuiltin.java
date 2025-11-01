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
package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpContext;

/** Built-in PHP function: constant($constant_name) Returns the value of a constant. */
public final class ConstantBuiltin extends PhpBuiltinRootNode {

  public ConstantBuiltin(PhpLanguage language) {
    super(language, "constant");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {

    // Require exactly 1 argument
    if (args.length < 1) {
      throw new RuntimeException("constant() expects exactly 1 argument");
    }

    // Get constant name (must be string)
    Object nameArg = args[0];
    if (!(nameArg instanceof String)) {
      throw new RuntimeException("constant() expects parameter 1 to be string");
    }
    String name = (String) nameArg;

    // Get constant value from context
    PhpContext context = PhpContext.get(this);
    Object value = context.getConstant(name);

    if (value == null) {
      throw new RuntimeException("Undefined constant: " + name);
    }

    return value;
  }
}
