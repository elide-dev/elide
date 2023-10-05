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

package elide.model.util

import com.google.protobuf.Timestamp
import java.time.Instant
import java.util.*

/**
 * Utilities to convert between different time objects, particularly a standard Protocol Buffer [Timestamp] and Java's
 * time objects, such as [Instant] and [Date].
 */
public actual object InstantFactory {
  /**
   * Convert a Protocol Buffers [Timestamp] record to a Java [Instant].
   *
   * @param subject Subject timestamp to convert.
   * @return Converted Java Instant.
   */
  @JvmStatic public fun instant(subject: Timestamp): Instant {
    return Instant.ofEpochSecond(subject.seconds, if (subject.nanos > 0) subject.nanos.toLong() else 0)
  }

  /**
   * Convert a Protocol Buffers [Timestamp] record to a Java [Date].
   *
   * @param subject Subject timestamp to convert.
   * @return Converted Java Date.
   */
  @JvmStatic public fun date(subject: Timestamp): Date {
    return Date.from(instant(subject))
  }
}
