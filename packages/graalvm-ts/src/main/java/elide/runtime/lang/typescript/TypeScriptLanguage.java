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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.TruffleLanguage.Registration;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSEngine;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import elide.runtime.lang.javascript.DelegatedModuleLoaderRegistry;
import elide.runtime.typescript.TypeScriptPrecompiler;
import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import org.graalvm.polyglot.SandboxPolicy;
import org.jetbrains.annotations.NotNull;

/**
 * TypeScript language implementation for GraalVM, meant for use via Elide.
 *
 * <p>The TypeScript language implementation uses GraalJs internally, and an embedded version of the
 * TypeScript Compiler (known as 'tsc'). The compiler is loaded into a dedicated JavaScript context
 * and realm, and granted I/O access in order to load scripts from disk sources.
 *
 * <p>At this time, Elide's implementation of TypeScript incurs a penalty to compile the input code
 * (or code loaded from modules) through `tsc`; later, this restriction may be lifted. TypeScript
 * does not support I/O isolation yet.
 */
@Registration(
    id = TypeScriptLanguage.ID,
    name = TypeScriptLanguage.NAME,
    implementationName = TypeScriptLanguage.IMPLEMENTATION_NAME,
    version = TypeScriptLanguage.TYPESCRIPT_VERSION,
    dependentLanguages = "js",
    defaultMimeType = TypeScriptLanguage.APPLICATION_MIME_TYPE,
    website = "https://docs.elide.dev",
    fileTypeDetectors = TypeScriptFileTypeDetector.class,
    contextPolicy = ContextPolicy.SHARED,
    sandbox = SandboxPolicy.TRUSTED,
    characterMimeTypes = {
      TypeScriptLanguage.TEXT_MIME_TYPE,
      TypeScriptLanguage.APPLICATION_MIME_TYPE,
      TypeScriptLanguage.MODULE_MIME_TYPE
    })
public class TypeScriptLanguage extends TruffleLanguage<JSRealm> {
  public static final String TEXT_MIME_TYPE = "text/typescript";
  public static final String APPLICATION_MIME_TYPE = "application/typescript";
  public static final String MODULE_MIME_TYPE = "application/typescript+module";
  public static final String NAME = "TypeScript";
  public static final String IMPLEMENTATION_NAME = "TypeScript";
  public static final String ID = "ts";
  public static final String TYPESCRIPT_VERSION = "5.8.2";
  private static final SortedSet<String> tsExtensions = new TreeSet<>();

  static {
    tsExtensions.add("ts");
    tsExtensions.add("tsx");
    tsExtensions.add("cts");
    tsExtensions.add("mts");
  }

  private static final TypeScriptPrecompiler tsPrecompiler = TypeScriptPrecompiler.obtain();
  private static final TypeScriptPrecompiledLoaderFactory tsLoaderFactory =
      new TypeScriptPrecompiledLoaderFactory();

  @CompilerDirectives.CompilationFinal private volatile TypeScriptPrecompiledLoader tsLoader = null;

  private final AtomicBoolean compilerInitialized = new AtomicBoolean(false);

  public static class TypeScriptPrecompiledLoaderFactory
      implements DelegatedModuleLoaderRegistry.DelegateFactory {
    @Override
    public @NotNull JSModuleLoader invoke(@NotNull JSRealm realm) {
      return new TypeScriptPrecompiledLoader(realm, tsPrecompiler);
    }

    @Override
    public boolean test(
        DelegatedModuleLoaderRegistry.DelegatedModuleRequest delegatedModuleRequest) {
      var src = delegatedModuleRequest.source();
      if (src != null) {
        return ID.equals(src.getLanguage());
      }
      var name = delegatedModuleRequest.label();
      var fileExt = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
      return tsExtensions.contains(fileExt);
    }
  }

  private void initializeJsRealm(Env currentEnv) {
    LanguageInfo jsInfo = currentEnv.getInternalLanguages().get("js");
    currentEnv.initializeLanguage(jsInfo);
  }

  private @NotNull JSRealm createContextForNativeEngine(Env currentEnv) {
    initializeJsRealm(currentEnv);
    var jsEnv = JavaScriptLanguage.getCurrentEnv();
    var js = JavaScriptLanguage.getCurrentLanguage();
    var ctx = JSEngine.createJSContext(js, jsEnv);
    var realm = ctx.createRealm(jsEnv);
    if (!compilerInitialized.get()) {
      compilerInitialized.compareAndSet(false, true);
      tsLoader = new TypeScriptPrecompiledLoader(realm, tsPrecompiler);
      DelegatedModuleLoaderRegistry.register(tsLoaderFactory);
    }
    return realm;
  }

  @Override
  protected @NotNull JSRealm createContext(Env currentEnv) {
    CompilerAsserts.neverPartOfCompilation();
    return createContextForNativeEngine(currentEnv);
  }

  @Override
  protected void finalizeContext(JSRealm context) {
    tsLoader = null;
    super.finalizeContext(context);
  }

  private CallTarget parseWithNativeEngine(ParsingRequest parsingRequest) throws IOException {
    List<String> argumentNames = parsingRequest.getArgumentNames();
    Source tsSource = parsingRequest.getSource();
    var env = JavaScriptLanguage.getCurrentEnv();
    var path = tsSource.getPath();
    var truffleFile = path != null ? env.getPublicTruffleFile(path) : null;
    Source jsSource = tsLoader.transpileSource(tsSource, truffleFile);
    var parsed = (RootCallTarget) env.parseInternal(jsSource, argumentNames.toArray(new String[0]));
    return parsed.getRootNode().getCallTarget();
  }

  @TruffleBoundary
  @Override
  protected CallTarget parse(ParsingRequest parsingRequest) throws IOException {
    return parseWithNativeEngine(parsingRequest);
  }

  @Override
  protected boolean patchContext(JSRealm context, Env newEnv) {
    // Restore the TypeScript loader, which can't be persisted to the aux cache.
    tsLoader = new TypeScriptPrecompiledLoader(context, tsPrecompiler);
    return true;
  }
}
