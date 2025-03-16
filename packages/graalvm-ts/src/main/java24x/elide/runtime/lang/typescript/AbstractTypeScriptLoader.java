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
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import java.io.IOException;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

abstract class AbstractTypeScriptLoader extends NpmCompatibleESModuleLoader {
  protected AbstractTypeScriptLoader(JSRealm realm) {
    super(realm);
  }

  @SuppressWarnings("SameParameterValue")
  @NotNull private JSModuleRecord tryLoadTypeScriptExt(
      @NotNull ScriptOrModule referrer,
      @NotNull String originalSpecifier,
      @NotNull Module.ModuleRequest moduleRequest,
      @NotNull String ext) {
    var specifier = originalSpecifier + "." + ext;
    var specifierTS = TruffleString.fromJavaStringUncached(specifier, TruffleString.Encoding.UTF_8);
    var tsModuleRequest =
        com.oracle.js.parser.ir.Module.ModuleRequest.create(
            specifierTS, moduleRequest.getAttributes());

    return super.resolveImportedModule(referrer, tsModuleRequest);
  }

  @SuppressWarnings("SameParameterValue")
  @NotNull private JSModuleRecord tryLoadTypeScriptDir(
      @NotNull ScriptOrModule referrer,
      @NotNull String originalSpecifier,
      @NotNull Module.ModuleRequest moduleRequest,
      @NotNull String file) {
    var specifier = originalSpecifier + "/" + file;
    var specifierTS = TruffleString.fromJavaStringUncached(specifier, TruffleString.Encoding.UTF_8);
    var tsModuleRequest = Module.ModuleRequest.create(specifierTS, moduleRequest.getAttributes());
    return super.resolveImportedModule(referrer, tsModuleRequest);
  }

  @Nullable private JSModuleRecord loadAsTypeScriptFallback(
      @NotNull ScriptOrModule referrer,
      @NotNull Module.ModuleRequest request,
      @Nullable JSException e1) {
    var originalSpecifier = request.getSpecifier().toJavaStringUncached();

    try {
      // maybe it's a naked import, and we can find it by appending `.ts`?
      if (!originalSpecifier.contains(".")) {
        return tryLoadTypeScriptExt(referrer, originalSpecifier, request, "ts");
      }
    } catch (JSException e2) {
      // failed; proceed to dir attempt?
    }

    try {
      // is it a directory?
      if (!originalSpecifier.contains(".")) {
        return tryLoadTypeScriptDir(referrer, originalSpecifier, request, "index.ts");
      }
    } catch (JSException e2) {
      // failed to load as dir
    }

    // if we get this far, the module failed to load, despite best efforts. we should throw the
    // original exception.
    if (e1 != null) {
      throw e1;
    }
    return null;
  }

  @Override
  @NotNull public JSModuleRecord resolveImportedModule(
      ScriptOrModule referrer, Module.ModuleRequest moduleRequest) {
    try {
      var env = JavaScriptLanguage.getCurrentEnv();
      var specifier = moduleRequest.getSpecifier().toJavaStringUncached();
      var parentSrc = referrer.getSource();
      var maybeParentPath = parentSrc.getPath();
      var maybeParentUri = parentSrc.getURI();
      TruffleFile maybeFile = null;
      if (maybeParentUri != null) {
        maybeParentPath = maybeParentUri.getPath();
      }
      if (maybeParentPath != null) {
        TruffleFile parentFile = env.getPublicTruffleFile(maybeParentPath).getParent();
        maybeFile = parentFile.resolve(specifier);
      }

      // did we find the import?
      if (maybeFile != null) {
        try {
          return loadModuleFromUrl(referrer, moduleRequest, maybeFile, null);
        } catch (IOException e) {
          // failed to load; fallback to normal behavior
        }
      }

      // we don't know what to do; fallback to default behavior
      return super.resolveImportedModule(referrer, moduleRequest);
    } catch (JSException e1) {
      // maybe the module is nakedly imported and is actually typescript? or maybe it's a directory
      // with `index.ts`?
      var mod = loadAsTypeScriptFallback(referrer, moduleRequest, e1);
      assert mod != null;
      return mod;
    }
  }

  @Override
  @NotNull public JSModuleRecord loadModule(Source source, JSModuleData moduleData) {
    return super.loadModule(source, moduleData);
  }

  @Override
  @NotNull protected JSModuleRecord loadModuleFromUrl(
      ScriptOrModule referrer,
      Module.ModuleRequest moduleRequest,
      TruffleFile maybeModuleFile,
      @Nullable String maybeCanonicalPath)
      throws IOException {
    if (maybeModuleFile != null && maybeModuleFile.exists()) {
      var canonicalPath = maybeModuleFile.getCanonicalFile().getPath();
      var maybeModuleMapEntry = moduleMap.get(canonicalPath);
      if (maybeModuleMapEntry != null) {
        return maybeModuleMapEntry;
      }

      Source source = transpileModule(referrer, moduleRequest, maybeModuleFile, canonicalPath);
      JSModuleData parsedModule = realm.getContext().getEvaluator().envParseModule(realm, source);
      var module = new JSModuleRecord(parsedModule, this);
      moduleMap.put(canonicalPath, module);
      return module;
    }
    return super.loadModuleFromUrl(referrer, moduleRequest, maybeModuleFile, maybeCanonicalPath);
  }

  @NotNull abstract Source transpileModule(
      ScriptOrModule referrer, Module.ModuleRequest request, TruffleFile maybeFile, String name)
      throws IOException;

  @NotNull abstract Source transpileSource(@NotNull Source sourceFile, @Nullable TruffleFile file)
      throws IOException;
}
