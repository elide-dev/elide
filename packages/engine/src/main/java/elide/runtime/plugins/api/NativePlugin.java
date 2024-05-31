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
package elide.runtime.plugins.api;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

/** TBD. */
public abstract class NativePlugin implements NativePluginAPI {
  private final String id;
  private static final Engine PLUGIN_ENGINE =
      Engine.newBuilder().allowExperimentalOptions(true).build();

  protected NativePlugin(String pluginId) {
    this.id = pluginId;
  }

  public String getPluginId() {
    return id;
  }

  public void context(Engine engine, Context.Builder builder, String[] args) {
    // (nothing by default)
  }

  @Override
  public void init() {
    // (nothing by default)
  }

  protected void apply(String[] args) {
    context(PLUGIN_ENGINE, initialize(this), args);
  }

  public static Context.Builder initialize(NativePlugin plugin) {
    var builder =
        Context.newBuilder()
            .allowExperimentalOptions(true)
            .allowNativeAccess(true)
            .engine(PLUGIN_ENGINE);

    plugin.context(PLUGIN_ENGINE, builder, new String[0]);
    return builder;
  }
}
