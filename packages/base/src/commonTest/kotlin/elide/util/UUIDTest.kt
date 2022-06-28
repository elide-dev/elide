package elide.util

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Tests for generating UUIDs via [UUID]. */
class UUIDTest {
  @Test fun testGenerateUuidRandom() {
    val generated = UUID.random()
    assertNotNull(
      generated,
      "should not get `null` for generated UUID"
    )
    for (i in 0 until 10) {
      assertNotEquals(
        generated,
        UUID.random(),
        "should not get same ID for consecutive calls to `UUID.random`"
      )
    }
  }

  @Test fun testGenerateUuidConsistentCasing() {
    val generated = UUID.random()
    assertNotNull(
      generated,
      "should not get `null` for generated UUID"
    )
    // there should be no lowercase letters in the UUID
    assertNull(
      generated.find { it.isLowerCase() },
      "should not have lowercase letters in generated UUID"
    )
  }
}
