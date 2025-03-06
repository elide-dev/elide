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

package elide.tool.cli;

import elide.runtime.gvm.JNIHeaderDirectives;
import elide.runtime.lang.Language;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CConstant;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;


/**
 * Shared-library definition for Elide's main CLI.
 */
@CContext(ElideSharedMain.EntryApiDirectives.class)
public class ElideSharedMain {
  static class EntryApiDirectives implements CContext.Directives {
    @Override
    public List<String> getOptions() {
      var allOptions = new LinkedList<String>();
      allOptions.addAll(JNIHeaderDirectives.getHeaderOptions());
      allOptions.addAll(Language.PluginApiDirectives.getHeaderOptions());
      return allOptions;
    }

    @Override
    public List<String> getHeaderFiles() {
      var allHeaders = new LinkedList<String>();
      allHeaders.addAll(JNIHeaderDirectives.getHeaders());
      allHeaders.addAll(Language.PluginApiDirectives.getHeaders());
      allHeaders.add(
        "\"" + Path.of(System.getProperty("elide.natives.entryApiHeader")).toAbsolutePath() + "\""
      );
      return allHeaders;
    }
  }

  @CConstant("ELIDE_ENTRY_API_VERSION")
  protected static native int getEntryApiVersion();

  @CStruct("el_entry_invocation")
  public interface NativeEntryInvocation extends PointerBase {
    @CField("f_apiversion")
    int getApiVersion();
  }
  private ElideSharedMain() {}

  /**
   * Native entrypoint for early runtime initialization; the returned integer is a version.
   *
   * @param thread the current isolate thread
   */
  @CEntryPoint(name = "elide_main_init")
  public static int initialize(@CEntryPoint.IsolateThreadContext IsolateThread thread) {
    return getEntryApiVersion();
  }

  /**
   * Native entrypoint for Elide's CLI as a native shared library; this entrypoint accepts a struct of args prepared by
   * the outer native launcher code, and otherwise boots itself.
   *
   * @param thread the current isolate thread
   * @param invocation the native entrypoint invocation struct
   */
  @CEntryPoint(name = "elide_main_entry")
  public static int entry(
          @CEntryPoint.IsolateThreadContext IsolateThread thread,
          NativeEntryInvocation invocation) {
    var args = ProcessHandle.current().info().arguments().orElseThrow();
    return NativeEntry.enter(args);
  }
}
