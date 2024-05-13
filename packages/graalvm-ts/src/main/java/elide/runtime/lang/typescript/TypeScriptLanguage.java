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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.TruffleLanguage.Registration;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSEngine;
import com.oracle.truffle.js.runtime.JSLanguageOptions;
import com.oracle.truffle.js.runtime.JSRealm;
import elide.runtime.lang.typescript.internals.JSRealmPatcher;
import elide.runtime.lang.typescript.internals.TypeScriptCompiler;
import elide.runtime.lang.typescript.internals.TypeScriptFileTypeDetector;
import elide.runtime.lang.typescript.internals.TypeScriptModuleLoader;
import org.graalvm.polyglot.SandboxPolicy;

import java.util.List;

/**
 * TBD.
 */
@Registration(
        id = "ts",
        name = "TypeScript",
        implementationName = "Elide TypeScript",
        version = "5.4.5",
        dependentLanguages = "js",
        characterMimeTypes = "application/typescript",
        website = "https://docs.elide.dev",
        fileTypeDetectors = TypeScriptFileTypeDetector.class,
        contextPolicy = ContextPolicy.SHARED,
        sandbox = SandboxPolicy.UNTRUSTED)
public class TypeScriptLanguage extends TruffleLanguage<JSRealm> {
  public static final String TEXT_MIME_TYPE = "text/typescript";
  public static final String APPLICATION_MIME_TYPE = "application/typescript";
  public static final String MODULE_MIME_TYPE = "application/typescript+module";
  public static final String NAME = "TypeScript";
  public static final String IMPLEMENTATION_NAME = "TypeScript";
  public static final String ID = "ts";
  private TypeScriptCompiler tsCompiler;
  private Env env;

  @Override
  protected JSRealm createContext(Env env) {
    CompilerAsserts.neverPartOfCompilation();
    var javaScriptLanguage = JavaScriptLanguage.getCurrentLanguage();
    LanguageInfo jsInfo = env.getInternalLanguages().get("js");
    env.initializeLanguage(jsInfo);
    var jsEnv = JavaScriptLanguage.getCurrentEnv();
    var ctx = JSEngine.createJSContext(javaScriptLanguage, jsEnv);
    tsCompiler = new TypeScriptCompiler(env);
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

  @Override
  protected CallTarget parse(ParsingRequest parsingRequest) {
    Source tsSource = parsingRequest.getSource();
    Source jsSource = tsCompiler.compileToNewSource(tsSource.getCharacters(), tsSource.getName(), true, tsSource.getPath());
    List<String> argumentNames = parsingRequest.getArgumentNames();
    var parsed = (RootCallTarget) env.parseInternal(jsSource, argumentNames.toArray(new String[0]));
    var wrapper = new TSRootNode(this, parsed.getRootNode());
    return wrapper.getCallTarget();
  }

  private class TSRootNode extends RootNode {
    private final RootNode delegate;

    protected TSRootNode(TruffleLanguage<?> language, RootNode delegate) {
      super(language);
      this.delegate = delegate;
    }

    @Override
    public Object execute(VirtualFrame frame) {
      JSRealm realm = JSRealm.get(delegate);
      JSRealmPatcher.setTSModuleLoader(realm, new TypeScriptModuleLoader(realm, tsCompiler));
      return delegate.execute(frame);
    }
  }
}
