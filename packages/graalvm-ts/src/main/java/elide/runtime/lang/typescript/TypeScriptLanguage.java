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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.TruffleLanguage.Registration;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSEngine;
import com.oracle.truffle.js.runtime.JSRealm;

import java.util.List;
import org.graalvm.polyglot.SandboxPolicy;

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
  public static final String TYPESCRIPT_VERSION = "5.4.5";
  private TypeScriptCompiler tsCompiler;
  private Env env;

  @Override
  protected JSRealm createContext(Env env) {
    CompilerAsserts.neverPartOfCompilation();
    var js = JavaScriptLanguage.getCurrentLanguage();
    LanguageInfo jsInfo = env.getInternalLanguages().get("js");
    env.initializeLanguage(jsInfo);
    var jsEnv = JavaScriptLanguage.getCurrentEnv();
    var ctx = JSEngine.createJSContext(js, jsEnv);
    tsCompiler = new TypeScriptCompiler(jsEnv);
    this.env = jsEnv;
    var realm = ctx.createRealm(jsEnv);
    JSRealmPatcher.setTSModuleLoader(realm, new TypeScriptModuleLoader(realm, tsCompiler));
    return realm;
  }

  @Override
  protected void finalizeContext(JSRealm context) {
    super.finalizeContext(context);
    tsCompiler.close();
  }

  @TruffleBoundary
  @Override
  protected CallTarget parse(ParsingRequest parsingRequest) {
    Source tsSource = parsingRequest.getSource();
    Source jsSource =
        tsCompiler.compileToNewSource(
            tsSource.getCharacters(), tsSource.getName(), true, tsSource.getPath());
    List<String> argumentNames = parsingRequest.getArgumentNames();
    var parsed = (RootCallTarget) env.parseInternal(jsSource, argumentNames.toArray(new String[0]));
    var wrapper = new TSRootNode(this, parsed.getRootNode());
    return wrapper.getCallTarget();
  }

  private static class TSRootNode extends RootNode {
    private final RootNode delegate;

    protected TSRootNode(TruffleLanguage<?> language, RootNode delegate) {
      super(language);
      this.delegate = delegate;
    }

    @Override
    public Object execute(VirtualFrame frame) {
      // @TODO(sgammon): causes restricted types to be reached
      // JSRealm realm = JSRealm.get(delegate);
      // JSRealmPatcher.setTSModuleLoader(realm, new TypeScriptModuleLoader(realm, tsCompiler));
      return delegate.execute(frame);
    }
  }
}
