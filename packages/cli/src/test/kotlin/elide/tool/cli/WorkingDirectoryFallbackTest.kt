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
      // Remove user.dir so the fallback logic engages
      System.clearProperty("user.dir")
      val cls = Class.forName("elide.tool.cli.MainKt")
      val m = cls.getDeclaredMethod("safeWorkingDirectory")
      m.isAccessible = true
      val result = m.invoke(null) as String
      assertTrue(result.isNotBlank(), "safeWorkingDirectory should return a non-blank path")
    } finally {
      if (orig == null) System.clearProperty("user.dir") else System.setProperty("user.dir", orig)
    }
  }
}
