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

/**
 * Built-in function: str_replace Replaces all occurrences of search with replace in subject.
 * str_replace(search, replace, subject)
 */
public final class StrReplaceBuiltin extends PhpBuiltinRootNode {

  public StrReplaceBuiltin(PhpLanguage language) {
    super(language, "str_replace");
  }

  @Override
  protected Object executeBuiltin(Object[] args) {
    if (args.length < 3) {
      return "";
    }

    String search = args[0].toString();
    String replace = args[1].toString();
    String subject = args[2].toString();

    return subject.replace(search, replace);
  }
}
