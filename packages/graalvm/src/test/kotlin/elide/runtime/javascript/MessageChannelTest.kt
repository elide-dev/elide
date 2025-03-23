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
@file:Suppress("JSUnresolvedReference")

package elide.runtime.javascript

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.node.events.StandardEventName
import elide.testing.annotations.TestCase
import elide.vm.annotations.Polyglot

@TestCase internal class MessageChannelTest : AbstractJsIntrinsicTest<MessageChannelBuiltin>() {
  override fun provide(): MessageChannelBuiltin = MessageChannelBuiltin()

  @Test override fun testInjectable() {
    assertNotNull(provide())
  }

  @Test fun testCreateMessageChannelHost() {
    assertNotNull(provide().newInstance())
    val x = provide().newInstance()
    assertNotNull(x)
    assertNotNull(x.port1)
    assertNotNull(x.port2)
  }

  @Test fun testCreateMessageChannel() = dual {
    assertNotNull(provide().newInstance())
    val x = provide().newInstance()
    assertNotNull(x)
    assertNotNull(x.port1)
    assertNotNull(x.port2)
  }.guest {
    // language=JavaScript
    """
      const channel = new MessageChannel();
      test(channel).isNotNull();
      test(channel.port1).isNotNull();
      test(channel.port2).isNotNull();
    """
  }

  @Test fun testMessagePortProps() {
    assertNotNull(provide().newInstance())
    val x = provide().newInstance()
    val port = x.port1
    assertNotNull(port.toString())
    val port2 = x.port2
    assertNotNull(port2.toString())
  }

  @Test fun testPostMessageSimple() = dual {
    val channel = provide().newInstance()
    val msg = mapOf("hi" to 5)
    assertDoesNotThrow {
      channel.port1.postMessage(Value.asValue(msg))
    }
  }.guest {
    // language=JavaScript
    """
      const channel = new MessageChannel();
      const msg = {hi: 5};
      channel.port1.postMessage(msg);
    """
  }

  @Test fun testPortAddEventListener() = dual {
    val channel = provide().newInstance()
    val msg = mapOf("hi" to 5)
    var dispatched = false
    channel.port2.addEventListener(StandardEventName.MESSAGE) {
      dispatched = true
    }
    assertDoesNotThrow {
      channel.port1.postMessage(Value.asValue(msg))
    }
    assertTrue(dispatched)
  }.guest {
    // language=JavaScript
    """
      const channel = new MessageChannel();
      const msg = {hi: 5};
      let dispatched = false;
      channel.port2.addEventListener("message", () => {
        dispatched = true;
      });
      channel.port2.start();
      channel.port1.start();
      channel.port1.postMessage(msg);
      test(dispatched === true).isEqualTo(true);
    """
  }

  @Test fun testPortOnMessageHost() {
    val channel = provide().newInstance()
    val msg = mapOf("hi" to 5)
    val dispatched = AtomicBoolean(false)
    channel.port2.putMember("onmessage", Value.asValue(object: ProxyExecutable {
      @Polyglot
      override fun execute(arguments: Array<out Value>?): Value {
        dispatched.set(true)
        return Value.asValue(null)
      }
    }))
    assertDoesNotThrow {
      channel.port1.postMessage(Value.asValue(msg))
    }
    assertTrue(dispatched.get())
  }

  @Test fun testPortOnMessage() = executeGuest {
    // language=JavaScript
    """
      const channel = new MessageChannel();
      const msg = {hi: 5};
      let dispatched = false;
      channel.port2.onmessage = () => {
        dispatched = true;
      };
      channel.port1.postMessage(msg);
      test(dispatched === true).isEqualTo(true);
    """
  }.doesNotFail()
}
