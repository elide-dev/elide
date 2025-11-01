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
package dev.truffle.php

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.io.ByteArrayOutputStream

/**
 * Abstract base class for PHP language tests.
 *
 * Provides common test utilities and helper methods for executing PHP code
 * and asserting results.
 */
abstract class AbstractPhpTest {
    /**
     * Execute PHP code and return the output.
     *
     * @param code The PHP code to execute (should include <?php tag)
     * @return The output from the execution (stdout)
     */
    protected fun executePhp(code: String): String {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()

        Context.newBuilder("php")
            .out(outputStream)
            .err(errorStream)
            .option("engine.WarnInterpreterOnly", "false")
            .allowAllAccess(true)
            .build().use { context ->
                context.eval(Source.newBuilder("php", code, "script.php").build())
            }

        return outputStream.toString()
    }

    /**
     * Execute PHP code and return both stdout and stderr.
     *
     * @param code The PHP code to execute
     * @return Pair of (stdout, stderr)
     */
    protected fun executePhpWithError(code: String): Pair<String, String> {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()

        Context.newBuilder("php")
            .out(outputStream)
            .err(errorStream)
            .option("engine.WarnInterpreterOnly", "false")
            .allowAllAccess(true)
            .build().use { context ->
                context.eval(Source.newBuilder("php", code, "script.php").build())
            }

        return Pair(outputStream.toString(), errorStream.toString())
    }

    /**
     * Execute PHP code and expect an exception.
     *
     * @param code The PHP code to execute
     * @return The exception message
     */
    protected fun executePhpExpectingError(code: String): String {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()

        try {
            Context.newBuilder("php")
                .out(outputStream)
                .err(errorStream)
                .option("engine.WarnInterpreterOnly", "false")
                .allowAllAccess(true)
                .build().use { context ->
                    context.eval(Source.newBuilder("php", code, "script.php").build())
                }
            throw AssertionError("Expected an exception but none was thrown")
        } catch (e: Exception) {
            return e.message ?: "Unknown error"
        }
    }
}
