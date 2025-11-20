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
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import java.io.IOException;
import java.net.URI;
import org.jetbrains.annotations.NotNull;

abstract class AbstractTypeScriptLoader extends NpmCompatibleESModuleLoader {
  private static final String ELIDE_MODULE_PREFIX = "elide";
  private static final String NODE_MODULE_PREFIX = "node";
  private static final String DENO_MODULE_PREFIX = "deno";
  private static final String BUN_MODULE_PREFIX = "bun";

  protected AbstractTypeScriptLoader(JSRealm realm) {
    super(realm);
  }

  private static boolean isSpecialProtocolImport(String specifier) {
    if (!specifier.contains(":")) {
      return false;
    }
    int colonIndex = specifier.indexOf(':');

    String protocol = specifier.substring(0, colonIndex);
    return protocol.equals(ELIDE_MODULE_PREFIX)
        || protocol.equals(NODE_MODULE_PREFIX)
        || protocol.equals(DENO_MODULE_PREFIX)
        || protocol.equals(BUN_MODULE_PREFIX);
  }

  @Override
  @NotNull public AbstractModuleRecord resolveImportedModule(
      ScriptOrModule referrer, com.oracle.js.parser.ir.Module.ModuleRequest moduleRequest) {
    var specifier = moduleRequest.specifier().toJavaStringUncached();

    // delegate special protocol prefixes to the realm's main module loader (ElideUniversalJsModuleLoader),
    // as it knows how to handle synthetic modules
    if (isSpecialProtocolImport(specifier)) {
      JSModuleLoader realmLoader = realm.getModuleLoader();
      if (realmLoader != null && realmLoader != this) {
        try {
          return realmLoader.resolveImportedModule(referrer, moduleRequest);
        } catch (Exception e) {
          // continue with file-based resolution
        }
      }
    }

    try {
      return super.resolveImportedModule(referrer, moduleRequest);
    } catch (JSException e1) {
      var tsSpecifier = specifier + ".ts";
      var specifierTS =
          TruffleString.fromJavaStringUncached(tsSpecifier, TruffleString.Encoding.UTF_8);
      var tsModuleRequest =
          com.oracle.js.parser.ir.Module.ModuleRequest.create(
              specifierTS, moduleRequest.attributes());

      try {
        return super.resolveImportedModule(referrer, tsModuleRequest);
      } catch (JSException e2) {
        var indexTsSpecifier = specifier + "/index.ts";
        specifierTS = TruffleString.fromJavaStringUncached(indexTsSpecifier, TruffleString.Encoding.UTF_8);
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
