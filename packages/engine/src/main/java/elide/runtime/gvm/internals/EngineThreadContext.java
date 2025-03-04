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
package elide.runtime.gvm.internals;

import elide.runtime.exec.GuestExecutor;
import elide.runtime.gvm.EngineContextAPI;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.jetbrains.annotations.NotNull;

@Immutable
@NotThreadSafe
record EngineThreadContext(GuestExecutor executor, Engine engine, Context context)
    implements EngineContextAPI {
  @Override
  public @NotNull Engine engine() {
    return engine;
  }

  @Override
  public @NotNull Context context() {
    return context;
  }

  @Override
  public @NotNull GuestExecutor executor() {
    return executor;
  }
}
