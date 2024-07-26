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
package elide.runtime.exec

import com.google.common.util.concurrent.ListeningExecutorService
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * # Guest Executor
 *
 * Defines the API surface made available, and expected, for guest execution management; a guest executor is provided
 * for host-side use, in the execution of guest code, which should be background-invoked by a guest runtime.
 *
 * For example, when using async methods via Node APIs (like the `fs`) module, execution of method logic takes place on
 * a [GuestExecutor] instance, which may manage context switching for the polyglot runtime as needed.
 *
 * APIs that originate promise objects work this way too, executing on the [GuestExecutor] when their value is awaited.
 *
 * &nbsp;
 *
 * ## Executor Services
 *
 * Guest executors are, firstly, [Executor] instances; depending on the implementation assigned, execution may take
 * place in a background thread or in the calling thread (for instance, during testing).
 *
 * In "production," the executor service is typically multithreaded, and equipped with context lock management.
 *
 * &nbsp;
 *
 * ## Coroutine Scope and Context
 *
 * In addition to the [Executor] API, the [GuestExecutor] interface extends [CoroutineScope], providing a bridge to the
 * Kotlin Coroutines API.
 *
 * Co-routine scope may be enclosed to provide context for host-side execution.
 */
public interface GuestExecutor : ListeningExecutorService, ScheduledExecutorService, CoroutineContext {
  /** @return The co-routine dispatcher scope/context to use. */
  public val dispatcher: CoroutineDispatcher
}
