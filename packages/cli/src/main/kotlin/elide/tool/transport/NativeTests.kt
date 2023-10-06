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

package elide.tool.transport

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Requires
import io.netty.channel.epoll.Epoll
import io.netty.channel.kqueue.KQueue
import io.netty.handler.ssl.OpenSsl
import elide.tool.annotations.EmbeddedTest
import elide.tool.testing.SelfTest

/** Native transport availability test. */
@Requires(notOs = [Requires.Family.WINDOWS])
@Bean @EmbeddedTest class NativeTransportTest : SelfTest() {
  override suspend fun SelfTestContext.test() {
    assertTrue(listOf(
      Epoll.isAvailable(),
      KQueue.isAvailable(),
    ).any())
  }
}

/** Native transport availability test. */
@Requires(notOs = [Requires.Family.WINDOWS])
@Bean @EmbeddedTest class NativeCryptoTest : SelfTest() {
  override suspend fun SelfTestContext.test() {
    assertTrue(listOf(
      OpenSsl.isAvailable()
    ).any())
  }
}

/** Epoll should be available on linux. */
@Requires(os = [Requires.Family.LINUX])
@Bean @EmbeddedTest class EpollTest : SelfTest() {
  override suspend fun SelfTestContext.test() {
    assertTrue(Epoll.isAvailable(), "epoll should be available on linux")
  }
}

/** KQueue should be available on macOS. */
@Requires(os = [Requires.Family.MAC_OS])
@Bean @EmbeddedTest class KQueueTest : SelfTest() {
  override suspend fun SelfTestContext.test() {
    assertTrue(KQueue.isAvailable(), "kqueue should be available on macos")
  }
}
