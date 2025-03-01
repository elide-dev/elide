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
package elide.runtime.lang.typescript;

import com.oracle.js.parser.ir.Module;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

@Deprecated
class TypeScriptModuleLoader extends AbstractTypeScriptLoader {
  private final TypeScriptCompiler tsCompiler;

  public TypeScriptModuleLoader(JSRealm realm, TypeScriptCompiler tsCompiler) {
    super(realm);
    this.tsCompiler = tsCompiler;
  }

  @Override
  @NotNull Source transpileModule(
      ScriptOrModule referrer, Module.ModuleRequest request, TruffleFile maybeFile, String name)
      throws IOException {
    var content = new String(maybeFile.readAllBytes(), StandardCharsets.UTF_8);
    return tsCompiler.compileToNewSource(
        content, request.getSpecifier().toJavaStringUncached(), true, name);
  }

  @Override
  @NotNull Source transpileSource(@NotNull Source sourceFile) {
    throw new UnsupportedOperationException("V1 TypeScript implementation only uses modules");
  }
}
