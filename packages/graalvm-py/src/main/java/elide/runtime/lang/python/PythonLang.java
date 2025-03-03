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

package elide.runtime.lang.python;

import elide.runtime.lang.Language;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CConstant;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CEntryPointLiteral;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.jetbrains.annotations.NotNull;

/**
 * Native Python plugin mappings.
 */
@CContext(Language.PluginApiDirectives.class)
public class PythonLang implements Language {
  @CConstant("ELIDE_PLUGIN_API_VERSION")
  protected static native int getPluginApiVersion();

  protected static CTypeConversion.CCharPointerHolder langIdHolder;

  protected static final CEntryPointLiteral<LangInitFunctionPointer> langInit = CEntryPointLiteral.create(
          PythonLang.class,
          "init",
          IsolateThread.class,
          NativeRuntimeInit.class);

  protected static final CEntryPointLiteral<LangEngineConfigureFunctionPointer> langEngine = CEntryPointLiteral.create(
          PythonLang.class,
          "configureEngine",
          IsolateThread.class,
          EngineConfigInvocation.class);

  protected static final CEntryPointLiteral<LangContextConfigureFunctionPointer> langCtx = CEntryPointLiteral.create(
          PythonLang.class,
          "configureContext",
          IsolateThread.class,
          ContextConfigInvocation.class);

  protected static final CEntryPointLiteral<LangEntryFunctionPointer> langEntry = CEntryPointLiteral.create(
          PythonLang.class,
          "entry",
          IsolateThread.class,
          NativeLangInvocation.class);

  @Override
  public @NotNull String getLanguageId() {
    return "python";
  }

  @CEntryPoint(name = "elide_lang_python_get_plugin_version")
  public static int getPluginVersion(IsolateThread thread) {
    return getPluginApiVersion();
  }

  @CEntryPoint(name = "elide_lang_python_load")
  public static void setup(IsolateThread thread, NativeLanguageInfo languageInfo) {
    var info = new PythonLang();
    langIdHolder = CTypeConversion.toCString(info.getLanguageId());
    languageInfo.setApiVersion(getPluginVersion(thread));
    languageInfo.setLangId(langIdHolder.get());
    languageInfo.setInitFunction(langInit.getFunctionPointer());
    languageInfo.setEngineConfigureFunction(langEngine.getFunctionPointer());
    languageInfo.setContextConfigureFunction(langCtx.getFunctionPointer());
    languageInfo.setEntryFunction(langEntry.getFunctionPointer());
  }

  @CEntryPoint(name = "elide_lang_python_init")
  public static void init(IsolateThread thread, NativeRuntimeInit init) {
    System.out.println("Initializing Python");
  }

  @CEntryPoint(name = "elide_lang_python_configure_engine")
  public static void configureEngine(IsolateThread thread, EngineConfigInvocation invocation) {
    System.out.println("Would configure engine Python");
  }

  @CEntryPoint(name = "elide_lang_python_configure_context")
  public static void configureContext(IsolateThread thread, ContextConfigInvocation invocation) {
    System.out.println("Would configure context for Python");
  }

  @CEntryPoint(name = "elide_lang_python_entry")
  public static void entry(IsolateThread thread, NativeLangInvocation invocation) {
    System.out.println("Would run Python");
  }
}
