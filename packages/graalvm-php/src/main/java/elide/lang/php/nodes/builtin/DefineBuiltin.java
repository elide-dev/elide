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
package elide.lang.php.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import elide.lang.php.PhpLanguage;
import elide.lang.php.nodes.PhpBuiltinRootNode;
import elide.lang.php.runtime.PhpContext;

/**
 * Built-in PHP function: define($name, $value, $case_insensitive = false) Defines a named constant.
 */
public final class DefineBuiltin extends PhpBuiltinRootNode {

  public DefineBuiltin(PhpLanguage language) {
    super(language, "define");
  }

  @Override
  @TruffleBoundary
  protected Object executeBuiltin(Object[] args) {

    // Require at least 2 arguments (name and value)
    if (args.length < 2) {
      throw new RuntimeException("define() expects at least 2 arguments");
    }

    // Get constant name (must be string)
    Object nameArg = args[0];
    if (!(nameArg instanceof String)) {
      throw new RuntimeException("define() expects parameter 1 to be string");
    }
    String name = (String) nameArg;

    // Get constant value (can be any type)
    Object value = args[1];

    // Optional case_insensitive parameter (deprecated in PHP 7.3+, we'll ignore it)
    // PHP constants are case-sensitive by default

    // Define the constant in the context
    PhpContext context = PhpContext.get(this);
    context.defineConstant(name, value);

    // define() returns true on success
    return true;
  }
}
