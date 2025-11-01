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
package dev.truffle.php.benchmark

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Test suite for running benchmarks and tracking performance.
 */
class BenchmarkTest {

    @Test
    fun `run micro benchmarks and generate report`() {
        val runner = BenchmarkRunner(
            warmupIterations = 5,
            measurementIterations = 20,
            verbose = true
        )

        val benchmarksDir = File("src/test/resources/benchmarks/micro")

        if (!benchmarksDir.exists() || benchmarksDir.listFiles()?.isEmpty() == true) {
            println("No benchmarks found in ${benchmarksDir.absolutePath}")
            return
        }

        val results = mutableListOf<BenchmarkRunner.BenchmarkResult>()

        // Run each benchmark
        benchmarksDir.listFiles()?.filter { it.extension == "php" }?.forEach { file ->
            println("\n=== Running benchmark: ${file.name} ===")
            val result = runner.runBenchmarkFile(file)
            results.add(result)

            println("""
                |Results for ${file.name}:
                |  Mean:       ${formatTime(result.mean)}
                |  Median:     ${formatTime(result.median)}
                |  Std Dev:    ${formatTime(result.stdDev)}
                |  Min:        ${formatTime(result.min.toDouble())}
                |  Max:        ${formatTime(result.max.toDouble())}
                |  Throughput: ${"%.2f".format(result.throughput)} ops/sec
            """.trimMargin())
        }

        // Generate summary report
        val suite = BenchmarkRunner.BenchmarkSuite("Micro Benchmarks", results)

        println("\n" + "=".repeat(80))
        println("BENCHMARK SUMMARY")
        println("=".repeat(80))
        println(suite.toSummaryTable())

        // Save report to file
        val reportFile = File("benchmark-report.md")
        reportFile.writeText("""
            |# TrufflePHP Benchmark Report
            |
            |${suite.toMarkdown()}
            |
            |## Summary
            |
            |${suite.toSummaryTable()}
        """.trimMargin())

        println("\nReport saved to: ${reportFile.absolutePath}")

        // Basic performance assertions
        results.forEach { result ->
            // Ensure benchmarks complete within reasonable time
            assertTrue(result.mean < 1_000_000_000, // Less than 1 second
                "Benchmark ${result.name} took too long: ${result.mean}ns")
        }
    }

    private fun formatTime(nanos: Double): String {
        return when {
            nanos < 1000 -> "${"%.2f".format(nanos)} ns"
            nanos < 1_000_000 -> "${"%.2f".format(nanos / 1000)} μs"
            nanos < 1_000_000_000 -> "${"%.2f".format(nanos / 1_000_000)} ms"
            else -> "${"%.2f".format(nanos / 1_000_000_000)} s"
        }
    }
}
