package elide.model.util

import com.google.protobuf.Timestamp
import java.time.Instant
import java.util.Date

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
