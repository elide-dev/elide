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
import elide.runtime.lang.javascript.JavaScriptCompilerConfig;
import elide.runtime.precompiler.Precompiler;
import elide.runtime.typescript.TypeScriptPrecompiler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

class TypeScriptPrecompiledLoader extends AbstractTypeScriptLoader {
  private final TypeScriptPrecompiler precompiler;

  public TypeScriptPrecompiledLoader(JSRealm realm, TypeScriptPrecompiler precompiler) {
    super(realm);
    this.precompiler = precompiler;
  }

  @NotNull private String resolveTsType(
      @Nullable TruffleFile file, @NotNull String name, @Nullable Path path) {
    // typescript always defaults to esm
    var lang = "application/javascript+module";

    // unless it's explicitly a CJS file via `.cts` (other settings could override this someday)
    if (name.endsWith(".cts")
        || (file != null && file.endsWith(".cts"))
        || (path != null && path.endsWith(".cts"))) {
      lang = "application/javascript";
    }
    return lang;
  }

  @Override
  @NotNull Source transpileModule(
      ScriptOrModule referrer, Module.ModuleRequest request, TruffleFile maybeFile, String name)
      throws IOException {
    var path = Paths.get(maybeFile.getPath());
    var content = new String(maybeFile.readAllBytes(), StandardCharsets.UTF_8);
    String transpiled = transpileSource(name, content, path);
    var lang = resolveTsType(maybeFile, name, path);
    return Source.newBuilder("js", transpiled, name).mimeType(lang).build();
  }

  @Override
  @NotNull Source transpileSource(@NotNull Source sourceFile) throws IOException {
    var name = sourceFile.getName();
    var path = sourceFile.getPath() != null ? Paths.get(sourceFile.getPath()) : null;
    var chars = sourceFile.getCharacters();
    if (chars == null) {
      if (path == null) {
        throw new IllegalStateException("Source file has no path or characters");
      }
      chars = Files.readString(path);
    }
    var transpiled = transpileSource(name, chars, path);
    var lang = resolveTsType(null, name, path);
    return Source.newBuilder("js", transpiled, name).mimeType(lang).build();
  }

  private @NotNull String transpileSource(
      @NotNull String name, @NotNull CharSequence source, @Nullable Path path) {
    var config = JavaScriptCompilerConfig.getDEFAULT();
    var info = new Precompiler.PrecompileSourceInfo(name, path);
    var req = new Precompiler.PrecompileSourceRequest<>(info, config);
    String transpiled = precompiler.invoke(req, (String) source);
    assert transpiled != null;
    return transpiled;
  }
}
