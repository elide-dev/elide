/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.tool.cli

import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertTrue

@TestCase class WorkingDirectoryFallbackTest {
  @Test fun returnsNonEmptyWhenUserDirMissingOrBlank() {
    val orig = System.getProperty("user.dir")
    try {
      System.clearProperty("user.dir")
      val result = safeWorkingDirectoryForTest()
      assertTrue(result.isNotBlank(), "safeWorkingDirectory should return a non-blank path")
    } finally {
      if (orig == null) System.clearProperty("user.dir") else System.setProperty("user.dir", orig)
    }
  }
}
