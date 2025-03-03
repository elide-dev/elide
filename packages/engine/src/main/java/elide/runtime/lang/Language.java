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

package elide.runtime.lang;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;


/**
 * Language
 *
 * <p>Describes a native language loaded via a {@link LanguagePlugin}.</p>
 */
@CContext(Language.PluginApiDirectives.class)
public interface Language {
  class PluginApiDirectives implements CContext.Directives {

    @Override
    public List<String> getHeaderFiles() {
      /*
       * The header file with the C declarations that are imported. We use a helper class that
       * locates the file in our project structure.
       */
      return Collections.singletonList(
              "\"" + Path.of(System.getProperty("elide.natives.pluginApiHeader")).toAbsolutePath() + "\""
      );
    }
  }

  interface LangInitFunctionPointer extends CFunctionPointer {
    @InvokeCFunctionPointer
    void invoke(IsolateThread thread, PointerBase init);
  }

  interface LangEngineConfigureFunctionPointer extends CFunctionPointer {
    @InvokeCFunctionPointer
    void invoke(IsolateThread thread, PointerBase invoation);
  }

  interface LangContextConfigureFunctionPointer extends CFunctionPointer {
    @InvokeCFunctionPointer
    void invoke(IsolateThread thread, PointerBase invoation);
  }

  interface LangEntryFunctionPointer extends CFunctionPointer {
    @InvokeCFunctionPointer
    void invoke(IsolateThread thread, PointerBase invoation);
  }

  @CStruct("el_runtime_init")
  interface NativeRuntimeInit extends PointerBase {}

  @CStruct("el_lang_info")
  interface NativeLanguageInfo extends PointerBase {
    @CField("f_apiversion")
    int getApiVersion();

    @CField("f_apiversion")
    void setApiVersion(int value);

    @CField("f_lang_id")
    CCharPointer getLangId();

    @CField("f_lang_id")
    void setLangId(CCharPointer value);

    @CField("f_init")
    LangInitFunctionPointer getInitFunction();

    @CField("f_init")
    void setInitFunction(LangInitFunctionPointer initFn);

    @CField("f_engine")
    LangEngineConfigureFunctionPointer getEngineConfigureFunction();

    @CField("f_engine")
    void setEngineConfigureFunction(LangEngineConfigureFunctionPointer entryFn);

    @CField("f_context")
    LangContextConfigureFunctionPointer getContextConfigureFunction();

    @CField("f_context")
    void setContextConfigureFunction(LangContextConfigureFunctionPointer entryFn);

    @CField("f_entry")
    LangEntryFunctionPointer getEntryFunction();

    @CField("f_entry")
    void setEntryFunction(LangEntryFunctionPointer entryFn);
  }

  @CStruct("el_lang_invoke")
  interface NativeLangInvocation extends PointerBase {
    @CField("f_apiversion")
    int getApiVersion();

    @CField("f_truffle_engine_handle")
    ObjectHandle getEngine();

    @CField("f_truffle_context_handle")
    ObjectHandle getContext();

    @CField("f_elide_dispatch_handle")
    ObjectHandle getElideDispatch();
  }

  @CStruct("el_lang_engine_config")
  interface EngineConfigInvocation extends PointerBase {
    @CField("f_apiversion")
    int getApiVersion();

    @CField("f_truffle_engine_builder_handle")
    ObjectHandle getEngineBuilder();
  }

  @CStruct("el_lang_context_config")
  interface ContextConfigInvocation extends PointerBase {
    @CField("f_apiversion")
    int getApiVersion();

    @CField("f_truffle_engine_handle")
    ObjectHandle getEngine();

    @CField("f_truffle_context_builder_handle")
    ObjectHandle getContextBuilder();
  }

  /**
   * Get this language's string ID; this is a short string identifying the language.
   *
   * @return Language ID string.
   */
  @NotNull String getLanguageId();
}
