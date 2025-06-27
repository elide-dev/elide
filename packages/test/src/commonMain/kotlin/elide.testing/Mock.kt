/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 */

package elide.testing

/**
 * API mocking utilities for Elide tests.
 * Provides embedded WireMock server capabilities.
 */
public expect object Mock {
  /**
   * Create a mock server on the specified port.
   * @param port Port to bind to (0 for dynamic port assignment)
   * @return Mock server instance
   */
  public fun server(port: Int = 0): MockServer
}

/**
 * Mock HTTP server interface.
 */
public expect interface MockServer {
  /**
   * Start the mock server.
   */
  public fun start()
  
  /**
   * Stop the mock server.
   */
  public fun stop()
  
  /**
   * Get the actual port the server is running on.
   */
  public val port: Int
}
