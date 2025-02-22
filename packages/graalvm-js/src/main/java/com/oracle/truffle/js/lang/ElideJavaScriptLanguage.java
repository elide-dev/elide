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
package com.oracle.truffle.js.lang;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.runtime.*;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionValues;
import org.graalvm.polyglot.SandboxPolicy;

/**
 * Patched/extended {@link com.oracle.truffle.js.lang.JavaScriptLanguage} which injects Elide's {@link ElideJSRealm}.
 */
@ProvidedTags({
        StandardTags.StatementTag.class,
        StandardTags.RootTag.class,
        StandardTags.RootBodyTag.class,
        StandardTags.ExpressionTag.class,
        StandardTags.CallTag.class,
        StandardTags.ReadVariableTag.class,
        StandardTags.WriteVariableTag.class,
        StandardTags.TryBlockTag.class,
        DebuggerTags.AlwaysHalt.class,
        // Expressions
        JSTags.ObjectAllocationTag.class,
        JSTags.BinaryOperationTag.class,
        JSTags.UnaryOperationTag.class,
        JSTags.WriteVariableTag.class,
        JSTags.ReadElementTag.class,
        JSTags.WriteElementTag.class,
        JSTags.ReadPropertyTag.class,
        JSTags.WritePropertyTag.class,
        JSTags.ReadVariableTag.class,
        JSTags.LiteralTag.class,
        JSTags.FunctionCallTag.class,
        // Statements and builtins
        JSTags.BuiltinRootTag.class,
        JSTags.EvalCallTag.class,
        JSTags.ControlFlowRootTag.class,
        JSTags.ControlFlowBlockTag.class,
        JSTags.ControlFlowBranchTag.class,
        JSTags.DeclareTag.class,
        // Other
        JSTags.InputNodeTag.class,
})
@TruffleLanguage.Registration(
        id = JavaScriptLanguage.ID,
        name = JavaScriptLanguage.NAME,
        implementationName = "Elide/JS (" + JavaScriptLanguage.IMPLEMENTATION_NAME + ")",
        characterMimeTypes = {
          JavaScriptLanguage.APPLICATION_MIME_TYPE,
          JavaScriptLanguage.TEXT_MIME_TYPE,
          JavaScriptLanguage.MODULE_MIME_TYPE
        },
        defaultMimeType = JavaScriptLanguage.APPLICATION_MIME_TYPE,
        contextPolicy = TruffleLanguage.ContextPolicy.SHARED,
        dependentLanguages = "regex",
        fileTypeDetectors = JSFileTypeDetector.class,
        website = "https://docs.elide.dev/",
        sandbox = SandboxPolicy.UNTRUSTED)
public class ElideJavaScriptLanguage extends TruffleLanguage<JSRealm> {
  private static final JavaScriptLanguage DEFAULT_JS = new JavaScriptLanguage();

  @CompilerDirectives.CompilationFinal
  private volatile JavaScriptLanguage javascript;

  @Override
  protected JSRealm createContext(Env env) {
    CompilerAsserts.neverPartOfCompilation();
    LanguageInfo jsInfo = env.getInternalLanguages().get("js");
    env.initializeLanguage(jsInfo);
    var jsEnv = JavaScriptLanguage.getCurrentEnv();
    var js = JavaScriptLanguage.getCurrentLanguage();
    if (javascript == null) {
      javascript = js;
    }
    var ctx = JSEngine.createJSContext(js, jsEnv);
    return ctx.createRealm(jsEnv);
    // return ElideJSRealm.wrapping(javascript.createContext(env));
  }

  // -- Delegation -- //

  @Override
  protected void finalizeContext(JSRealm realm) {
    javascript.finalizeContext(realm);
  }

  @CompilerDirectives.TruffleBoundary
  @Override
  public CallTarget parse(ParsingRequest parsingRequest) {
    return javascript.parse(parsingRequest);
  }

  @Override
  protected ExecutableNode parse(InlineParsingRequest request) throws Exception {
    return javascript.parse(request);
  }

  @Override
  protected void disposeContext(JSRealm realm) {
    javascript.disposeContext(realm);
  }

  @Override
  protected void initializeMultipleContexts() {
    javascript.initializeMultipleContexts();
  }

  @Override
  protected boolean areOptionsCompatible(OptionValues firstOptions, OptionValues newOptions) {
    return DEFAULT_JS.areOptionsCompatible(firstOptions, newOptions);
  }

  @Override
  protected OptionDescriptors getOptionDescriptors() {
    // don't use `javascript` object field because it may not be ready yet
    return DEFAULT_JS.getOptionDescriptors();
  }

  @Override
  protected boolean isVisible(JSRealm realm, Object value) {
    return javascript.isVisible(realm, value);
  }

  @Override
  protected Object getLanguageView(JSRealm context, Object value) {
    return javascript.getLanguageView(context, value);
  }

  @Override
  protected Object getScope(JSRealm context) {
    return javascript.getScope(context);
  }
}
