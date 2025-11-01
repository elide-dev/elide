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

/** Built-in PHP function: defined($constant_name) Checks whether a given named constant exists. */
public final class DefinedBuiltin extends PhpBuiltinRootNode {

  public DefinedBuiltin(PhpLanguage language) {
    super(language, "defined");
  }

  @Override
  @TruffleBoundary
  protected Object executeBuiltin(Object[] args) {

    // Require exactly 1 argument
    if (args.length < 1) {
      throw new RuntimeException("defined() expects exactly 1 argument");
    }

    // Get constant name (must be string)
    Object nameArg = args[0];
    if (!(nameArg instanceof String)) {
      throw new RuntimeException("defined() expects parameter 1 to be string");
    }
    String name = (String) nameArg;

    // Check if constant is defined in the context
    PhpContext context = PhpContext.get(this);
    return context.isConstantDefined(name);
  }
}
