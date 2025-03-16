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

@file:Suppress("unused")

package benchmarks.context

import io.micronaut.context.ApplicationContext
import java.util.concurrent.TimeUnit
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import elide.annotations.Eager

@Suppress("DuplicatedCode")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class InjectionContextBenchmark {
  @Benchmark fun create(): ApplicationContext {
    val builder = ApplicationContext
      .builder()
      .environments("cli")
      .defaultEnvironments("cli")
      .eagerInitAnnotated(Eager::class.java)
      .eagerInitSingletons(false)
      .eagerInitConfiguration(true)
      .deduceEnvironment(false)
      .deduceCloudEnvironment(false)

    assert(builder != null)
    val ctx = builder.build()

    try {
      return ctx
    } finally {
      ctx.close()
    }
  }

  @Benchmark fun start(): ApplicationContext {
    val builder = ApplicationContext
      .builder()
      .environments("cli")
      .defaultEnvironments("cli")
      .eagerInitAnnotated(Eager::class.java)
      .eagerInitSingletons(false)
      .eagerInitConfiguration(true)
      .deduceEnvironment(false)
      .deduceCloudEnvironment(false)

    assert(builder != null)
    val ctx = builder.build()

    try {
      return ctx.start()
    } finally {
      ctx.close()
    }
  }
}
