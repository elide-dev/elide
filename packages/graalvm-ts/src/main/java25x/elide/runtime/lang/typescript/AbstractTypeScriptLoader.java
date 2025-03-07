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
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.commonjs.NpmCompatibleESModuleLoader;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.AbstractModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import java.io.IOException;
import java.net.URI;
import org.jetbrains.annotations.NotNull;

abstract class AbstractTypeScriptLoader extends NpmCompatibleESModuleLoader {
  protected AbstractTypeScriptLoader(JSRealm realm) {
    super(realm);
  }

  @Override
  @NotNull public AbstractModuleRecord resolveImportedModule(
      ScriptOrModule referrer, com.oracle.js.parser.ir.Module.ModuleRequest moduleRequest) {
    try {
      return super.resolveImportedModule(referrer, moduleRequest);
    } catch (JSException e1) {
      var originalSpecifier = moduleRequest.specifier().toJavaStringUncached();
      var specifier = originalSpecifier + ".ts";
      var specifierTS =
          TruffleString.fromJavaStringUncached(specifier, TruffleString.Encoding.UTF_8);
      var tsModuleRequest =
          com.oracle.js.parser.ir.Module.ModuleRequest.create(
              specifierTS, moduleRequest.attributes());

      try {
        return super.resolveImportedModule(referrer, tsModuleRequest);
      } catch (JSException e2) {
        specifier = originalSpecifier + "/index.ts";
        specifierTS = TruffleString.fromJavaStringUncached(specifier, TruffleString.Encoding.UTF_8);
        tsModuleRequest = Module.ModuleRequest.create(specifierTS, moduleRequest.attributes());
        return super.resolveImportedModule(referrer, tsModuleRequest);
      }
    }
  }

  @Override
  protected AbstractModuleRecord loadModuleFromFile(
      ScriptOrModule referrer,
      Module.ModuleRequest moduleRequest,
      TruffleFile moduleFile,
      String maybeCanonicalPath)
      throws IOException {
    if (moduleFile != null && moduleFile.exists() && moduleFile.endsWith(".ts")) {
      var canonicalPath = moduleFile.getCanonicalFile().getPath();
      var maybeModuleMapEntry = moduleMap.get(canonicalPath);
      if (maybeModuleMapEntry != null) {
        return maybeModuleMapEntry;
      }

      Source source = transpileModule(referrer, moduleRequest, moduleFile, canonicalPath);
      JSModuleData parsedModule = realm.getContext().getEvaluator().envParseModule(realm, source);
      var module = new JSModuleRecord(parsedModule, this);
      moduleMap.put(canonicalPath, module);
      return module;
    }
    return super.loadModuleFromFile(referrer, moduleRequest, moduleFile, maybeCanonicalPath);
  }

  @Override
  @NotNull protected AbstractModuleRecord loadModuleFromURL(
      ScriptOrModule referrer, Module.ModuleRequest moduleRequest, URI moduleURI)
      throws IOException {
    return super.loadModuleFromURL(referrer, moduleRequest, moduleURI);
  }

  @NotNull abstract Source transpileModule(
      ScriptOrModule referrer, Module.ModuleRequest request, TruffleFile maybeFile, String name)
      throws IOException;

  @NotNull abstract Source transpileSource(@NotNull Source sourceFile) throws IOException;
}
