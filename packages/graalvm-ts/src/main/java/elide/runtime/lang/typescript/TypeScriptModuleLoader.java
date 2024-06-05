/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class TypeScriptModuleLoader extends DefaultESModuleLoader {
  private final TypeScriptCompiler tsCompiler;

  public TypeScriptModuleLoader(JSRealm realm, TypeScriptCompiler tsCompiler) {
    super(realm);
    this.tsCompiler = tsCompiler;
  }

  @Override
  public JSModuleRecord resolveImportedModule(
      ScriptOrModule referrer, Module.ModuleRequest moduleRequest) {
    try {
      return super.resolveImportedModule(referrer, moduleRequest);
    } catch (JSException e1) {
      var originalSpecifier = moduleRequest.getSpecifier().toJavaStringUncached();
      var specifier = originalSpecifier + ".ts";
      var specifierTS =
          TruffleString.fromJavaStringUncached(specifier, TruffleString.Encoding.UTF_8);
      var tsModuleRequest = Module.ModuleRequest.create(specifierTS, moduleRequest.getAttributes());

      try {
        return super.resolveImportedModule(referrer, tsModuleRequest);
      } catch (JSException e2) {
        specifier = originalSpecifier + "/index.ts";
        specifierTS = TruffleString.fromJavaStringUncached(specifier, TruffleString.Encoding.UTF_8);
        tsModuleRequest = Module.ModuleRequest.create(specifierTS, moduleRequest.getAttributes());
        return super.resolveImportedModule(referrer, tsModuleRequest);
      }
    }
  }

  @Override
  protected JSModuleRecord loadModuleFromUrl(
      ScriptOrModule referrer,
      Module.ModuleRequest moduleRequest,
      TruffleFile maybeModuleFile,
      String maybeCanonicalPath)
      throws IOException {
    var maybeModuleFilePath = maybeModuleFile.getPath();
    if (maybeModuleFile.exists() && maybeModuleFilePath.endsWith(".ts")) {
      var canonicalPath = maybeModuleFile.getCanonicalFile().getPath();
      var maybeModuleMapEntry = moduleMap.get(canonicalPath);
      if (maybeModuleMapEntry != null) {
        return maybeModuleMapEntry;
      }

      var content = new String(maybeModuleFile.readAllBytes(), StandardCharsets.UTF_8);
      Source source =
          tsCompiler.compileToNewSource(
              content, moduleRequest.getSpecifier().toJavaStringUncached(), true, canonicalPath);
      JSModuleData parsedModule = realm.getContext().getEvaluator().envParseModule(realm, source);
      var module = new JSModuleRecord(parsedModule, this);
      moduleMap.put(canonicalPath, module);
      return module;
    }
    return super.loadModuleFromUrl(referrer, moduleRequest, maybeModuleFile, maybeCanonicalPath);
  }

  @Override
  public JSModuleRecord loadModule(Source source, JSModuleData moduleData) {
    return super.loadModule(source, moduleData);
  }
}
