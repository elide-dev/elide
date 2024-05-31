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
package elide.runtime.python;

import elide.runtime.plugins.api.NativePlugin;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.word.Pointer;

/** TBD. */
@SuppressWarnings("unused")
public class ElidePythonLanguage extends NativePlugin {
  private static final String ELIDE_PYTHON = "python";
  private static final ElidePythonLanguage SINGLETON = new ElidePythonLanguage();
  private static final Context BASE = initialize(SINGLETON).build();

  ElidePythonLanguage() {
    super(ELIDE_PYTHON);
  }

  public static ElidePythonLanguage get() {
    return SINGLETON;
  }

  @Override
  public void context(Engine engine, Context.Builder builder, String[] args) {
    builder.option("python.EmulateJython", "false");
    builder.option("python.NativeModules", "true");
    builder.option("python.LazyStrings", "true");
    builder.option("python.WithTRegex", "true");
    builder.option("python.WithCachedSources", "true");
    builder.option("python.HPyBackend", "nfi");
    builder.option("python.PosixModuleBackend", "java");
  }

  public static void main(String[] args) {
    get().apply(args);
  }

  @CEntryPoint(name = "Java_elide_runtime_python_ElidePythonLanguage_init")
  public static void init(
      Pointer jniEnv, Pointer clazz, @CEntryPoint.IsolateThreadContext long isolateId) {
    System.out.println("Native Python plugin initialized (isolate: " + isolateId + ")");
  }
}
