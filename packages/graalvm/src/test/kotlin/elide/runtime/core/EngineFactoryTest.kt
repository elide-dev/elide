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

package elide.runtime.core

import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.EngineCreated
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.internals.graalvm.GraalVMContext
import elide.runtime.core.internals.graalvm.GraalVMEngine

@OptIn(DelicateElideApi::class)
internal class EngineFactoryTest {
  @Test fun testEngineFactory() {
    var engineCreationIntercepted = false
    var contextCreationIntercepted = false
    var configurationCalled = false
    
    val plugin = object : EnginePlugin<Unit, Unit> {
      override val key: Key<Unit> = Key("TestPlugin")

      override fun install(scope: EnginePlugin.InstallationScope, configuration: Unit.() -> Unit) {
        configuration(Unit)
        
        scope.lifecycle.on(EngineCreated) { engineCreationIntercepted = true }
        scope.lifecycle.on(ContextCreated) { contextCreationIntercepted = true }
      }
    }
    
    val engine = PolyglotEngine { install(plugin) { configurationCalled = true } }
    val context = engine.acquire()
    
    assertIs<GraalVMEngine>(engine, "should return a GraalVM engine implementation")
    assertIs<GraalVMContext>(context, "should return a GraalVM context implementation")
    assertTrue(engineCreationIntercepted, "plugin should receive engine creation event")
    assertTrue(contextCreationIntercepted, "plugin should receive context creation event")
    assertTrue(configurationCalled, "plugin should receive configuration block from DSL")
  }
}
