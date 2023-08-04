/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.server.runtime

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.micronaut.context.annotation.Replaces
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import jakarta.inject.Singleton

/** Provides an implementation of [AppExecutor] that directly executes all tasks in the current thread. */
@Suppress("UnstableApiUsage")
@Replaces(AppExecutor.DefaultExecutor::class)
@Singleton public class TestAppExecutor: AppExecutor {
  override fun service(): ListeningScheduledExecutorService {
    return MoreExecutors.listeningDecorator(
      MoreExecutors.getExitingScheduledExecutorService(
        ScheduledThreadPoolExecutor(1, ThreadFactory {
          Thread.currentThread()
        })
      )
    )
  }
}
