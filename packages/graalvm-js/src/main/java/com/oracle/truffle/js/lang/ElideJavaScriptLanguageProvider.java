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

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.provider.TruffleLanguageProvider;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import org.graalvm.polyglot.SandboxPolicy;

import java.util.Collection;
import java.util.List;

/**
 * Patched JavaScript language provider; wired manually.
 */
@GeneratedBy(JavaScriptLanguage.class)
@TruffleLanguage.Registration(characterMimeTypes = {"application/javascript", "text/javascript", "application/javascript+module"}, contextPolicy = TruffleLanguage.ContextPolicy.SHARED, defaultMimeType = "application/javascript", dependentLanguages = "regex", id = "js", implementationName = "GraalVM JavaScript", name = "JavaScript", sandbox = SandboxPolicy.UNTRUSTED, website = "https://www.graalvm.org/javascript/")
@ProvidedTags({StandardTags.StatementTag.class, StandardTags.RootTag.class, StandardTags.RootBodyTag.class, StandardTags.ExpressionTag.class, StandardTags.CallTag.class, com.oracle.truffle.api.instrumentation.StandardTags.ReadVariableTag.class, com.oracle.truffle.api.instrumentation.StandardTags.WriteVariableTag.class, StandardTags.TryBlockTag.class, DebuggerTags.AlwaysHalt.class, JSTags.ObjectAllocationTag.class, JSTags.BinaryOperationTag.class, JSTags.UnaryOperationTag.class, com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag.class, JSTags.ReadElementTag.class, JSTags.WriteElementTag.class, JSTags.ReadPropertyTag.class, JSTags.WritePropertyTag.class, com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableTag.class, JSTags.LiteralTag.class, JSTags.FunctionCallTag.class, JSTags.BuiltinRootTag.class, JSTags.EvalCallTag.class, JSTags.ControlFlowRootTag.class, JSTags.ControlFlowBlockTag.class, JSTags.ControlFlowBranchTag.class, JSTags.DeclareTag.class, JSTags.InputNodeTag.class})
public final class ElideJavaScriptLanguageProvider extends TruffleLanguageProvider {
  @Override
  protected String getLanguageClassName() {
    return "com.oracle.truffle.js.lang.ElideJavaScriptLanguage";
  }

  @Override
  protected Object create() {
    return new ElideJavaScriptLanguage();
  }

  @Override
  protected Collection<String> getServicesClassNames() {
    return List.of();
  }

  @Override
  protected List<?> createFileTypeDetectors() {
    return List.of(new JSFileTypeDetector());
  }

  @Override
  protected List<String> getInternalResourceIds() {
    return List.of();
  }

  @Override
  protected Object createInternalResource(String resourceId) {
    throw new IllegalArgumentException(String.format("Unsupported internal resource id %s, supported ids are ", resourceId));
  }
}
