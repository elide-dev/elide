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
package elide.runtime.lang.javascript;

import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import java.lang.reflect.Field;

@Deprecated
public class JSRealmPatcher {
  @Deprecated
  public static void setModuleLoader(JSRealm jsRealm, JSModuleLoader newModuleLoader) {
    try {
      Field moduleLoaderField = JSRealm.class.getDeclaredField("moduleLoader");
      moduleLoaderField.setAccessible(true);
      Object moduleLoader = moduleLoaderField.get(jsRealm);
      if (moduleLoader != newModuleLoader) {
        moduleLoaderField.set(jsRealm, newModuleLoader);
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      assert false;
    }
  }
}
