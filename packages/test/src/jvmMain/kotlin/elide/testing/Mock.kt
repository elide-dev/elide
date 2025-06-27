/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 */

package elide.testing

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

/**
 * JVM implementation of API mocking utilities using WireMock.
 */
public actual object Mock {
    /**
     * Create a WireMock server on the specified port.
     */
    public actual fun server(port: Int): MockServer {
        return JvmMockServer(WireMockServer(options().port(port)))
    }
}

/**
 * JVM implementation wrapping WireMockServer.
 */
public actual interface MockServer {
    public actual fun start()
    public actual fun stop()
    public actual val port: Int
}

/**
 * JVM implementation of MockServer using WireMock.
 */
private class JvmMockServer(private val wireMockServer: WireMockServer) : MockServer {
    override fun start() = wireMockServer.start()
    override fun stop() = wireMockServer.stop()
    override val port: Int get() = wireMockServer.port()
}
