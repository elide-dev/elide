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

@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.intrinsics.js.express

import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.testing.annotations.TestCase

@TestCase internal class ExpressIntrinsicTest : AbstractJsIntrinsicTest<ExpressIntrinsic>() {
  @Inject lateinit var express: ExpressIntrinsic
  
  override fun provide(): ExpressIntrinsic = express

  override fun testInjectable() {
    assertNotNull(express) { "should be able to resolve express intrinsic via injection" }
  }
  
  @Test fun testInjectableGuest() = executeGuest {
    //language=javascript
    """
    express
    """
  }.thenAssert {
    assertNotNull(it.returnValue(), "should not get a null return value")
    assertTrue(it.returnValue()!!.canExecute(), "should get an executable as return value")
  }
  
  @Test fun testGuestCreateApp() = executeGuest {
    //language=javascript
    """
    express()
    """
  }.thenAssert {
    assertNotNull(it.returnValue(), "should not get a null return value")
    assertTrue(it.returnValue()!!.isHostObject, "should get a host object as return value")
    assertIs<ExpressAppIntrinsic>(
      value = it.returnValue()!!.asHostObject(),
      message = "should return host intrinsic"
    )
  }
  
  @Test fun testGuestRouteGet() = executeGuest {
    //language=javascript
    """
    express().get("/hello", (req, res) => { })
    """
  }.doesNotFail()
}
