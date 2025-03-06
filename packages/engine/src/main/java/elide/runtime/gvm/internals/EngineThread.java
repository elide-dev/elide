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

import elide.runtime.gvm.EngineContextAPI;
import elide.runtime.gvm.EngineThreadAPI;
import jakarta.annotation.Nullable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import org.jetbrains.annotations.NotNull;

// Implementation of an engine execution thread.
final class EngineThread extends ForkJoinWorkerThread implements EngineThreadAPI, AutoCloseable {
  // Record which holds thread-bound context for this engine thread.
  private final ThreadLocal<EngineThreadContext> threadContext = new ThreadLocal<>();

  // Whether this context is currently alive.
  private volatile boolean alive = false;

  // Whether this context has initialized (been attached to its assigned polyglot objects).
  private volatile boolean initialized = false;

  // Super-constructor.
  EngineThread(ThreadGroup group, ForkJoinPool pool, boolean preserveThreadLocals) {
    super(group, pool, preserveThreadLocals);
    assert preserveThreadLocals;
  }

  // Super-constructor.
  EngineThread(ForkJoinPool pool) {
    super(pool);
  }

  private @NotNull EngineContextAPI obtainThreadContext() {
    var ctx = threadContext.get();
    assert ctx != null;
    return ctx;
  }

  void attach(
      @NotNull EngineThreadContext ctx, @NotNull UncaughtExceptionHandler exceptionHandler) {
    assert !initialized;
    assert !alive;
    threadContext.set(ctx);
    initialized = true;
    setUncaughtExceptionHandler(exceptionHandler);
  }

  void shutdown() {
    obtainThreadContext().context().close();
  }

  void detach() {
    threadContext.remove();
    initialized = false;
  }

  @Override
  public @NotNull EngineContextAPI engineContext() {
    return obtainThreadContext();
  }

  @SuppressWarnings("resource")
  @Override
  protected void onStart() {
    assert initialized;
    assert !alive;
    alive = true;
    super.onStart();
    obtainThreadContext().context().enter();
  }

  @Override
  protected void onTermination(@Nullable Throwable exception) {
    shutdown();
    detach();
  }

  @Override
  public void close() throws Exception {
    onTermination(null);
  }

  @Override
  public void start() {
    super.start();
  }
}
