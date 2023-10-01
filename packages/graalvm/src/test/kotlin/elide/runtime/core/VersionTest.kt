package elide.runtime.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

@OptIn(DelicateElideApi::class)
class VersionTest {
  @Test fun testParseValidVersion() {
    val cases = arrayOf(
      "1.1.5" to Version(1, 1, 5),
      "5.1" to Version(5, 1, 0),
      "2.0.0-SNAPSHOT" to Version(2, 0, 0),
      "1-1-1" to Version(1, 1, 1),
      "0_4_1" to Version(0, 4, 1),
      "19-2_1" to Version(19, 2, 1),
    )

    for ((source, expected) in cases) assertEquals(
      expected,
      actual = Version.parse(source),
      message = "should parse valid version '$source'",
    )
  }

  @Test fun testParseInvalidVersion() {
    val cases = arrayOf(
      "1",
      "a5.1",
      "a.b.c",
      "1.5.c",
    )

    for (source in cases) assertThrows<IllegalArgumentException>("should reject invalid version '$source'") {
      Version.parse(source)
    }
  }

  @Test fun testCompareVersions() {
    val cases = arrayOf(
      Triple("1.5.0", "1.5.1", -1),
      Triple("1.5", "1.5.1", -1),
      Triple("1.5", "1.6.1", -1),
      Triple("1.5", "2.5", -1),
      Triple("1.5", "2.5.1", -1),
      Triple("1.5.0", "1.5.0", 0),
      Triple("1.5", "1.5.0", 0),
      Triple("1.5.0", "1.4.9", 1),
      Triple("2.5.0", "1.5.9", 1),
    )

    for ((left, right, result) in cases) assertEquals(
      expected = result,
      actual = Version.parse(left) compareTo Version.parse(right),
      message = "should compare correctly: '$left' vs '$right')",
    )
  }

  @Test fun testVersionInRange() {
    val range = Version(1, 2, 3).andHigher()
    val cases = arrayOf(
      "0.1" to false,
      "1.2.2" to false,
      "1.2.3" to true,
      "4.1" to true,
    )

    for ((source, result) in cases) assertEquals(
      expected = result,
      actual = Version.parse(source) in range,
      message = "should return $result for '$source' in '$range'",
    )
  }
}