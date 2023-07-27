package elide.runtime.gvm.internals.intrinsics.js.express

import elide.annotations.Inject
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.express.ExpressApp
import elide.testing.annotations.TestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestCase internal class ExpressIntrinsicTest : AbstractJsIntrinsicTest<ExpressIntrinsic>() {
  @Inject lateinit var express: ExpressIntrinsic
  
  override fun provide(): ExpressIntrinsic = express

  override fun testInjectable() {
    assertNotNull(express) { "should be able to resolve express intrinsic via injection" }
  }
  
  @Test fun testRouteMapping() {
    assertEquals(
      expected = "/hello/{name}",
      actual = ExpressAppIntrinsic.mapExpressToReactorRoute("/hello/:name"),
      message = "should replace path variable matchers"
    )
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
