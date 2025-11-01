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

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.sqrt
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Benchmark runner for TrufflePHP performance testing.
 *
 * Features:
 * - Warmup detection
 * - Statistical analysis (mean, median, std deviation)
 * - Memory usage tracking
 * - Comparison with baseline
 * - JSON output for CI integration
 */
class BenchmarkRunner(
    val warmupIterations: Int = 10,
    val measurementIterations: Int = 100,
    val trackMemory: Boolean = true,
    val verbose: Boolean = false
) {

    data class BenchmarkResult(
        val name: String,
        val warmupTime: Long,
        val measurements: List<Long>,
        val mean: Double,
        val median: Double,
        val stdDev: Double,
        val min: Long,
        val max: Long,
        val memoryBefore: Long?,
        val memoryAfter: Long?,
        val throughput: Double // ops/sec
    ) {
        fun toMarkdown(): String {
            return """
            |### $name
            |
            |**Performance:**
            |- Mean: ${formatTime(mean)}
            |- Median: ${formatTime(median)}
            |- Std Dev: ${formatTime(stdDev)}
            |- Min: ${formatTime(min.toDouble())}
            |- Max: ${formatTime(max.toDouble())}
            |- Throughput: ${"%.2f".format(throughput)} ops/sec
            |
            |**Memory:**
            |- Before: ${formatMemory(memoryBefore)}
            |- After: ${formatMemory(memoryAfter)}
            |- Delta: ${formatMemory(memoryAfter?.minus(memoryBefore ?: 0))}
            |
            """.trimMargin()
        }

        private fun formatTime(nanos: Double): String {
            return when {
                nanos < 1000 -> "${"%.2f".format(nanos)} ns"
                nanos < 1_000_000 -> "${"%.2f".format(nanos / 1000)} μs"
                nanos < 1_000_000_000 -> "${"%.2f".format(nanos / 1_000_000)} ms"
                else -> "${"%.2f".format(nanos / 1_000_000_000)} s"
            }
        }

        private fun formatMemory(bytes: Long?): String {
            if (bytes == null) return "N/A"
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${"%.2f".format(bytes / 1024.0)} KB"
                bytes < 1024 * 1024 * 1024 -> "${"%.2f".format(bytes / (1024.0 * 1024))} MB"
                else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
            }
        }
    }

    data class BenchmarkSuite(
        val name: String,
        val results: List<BenchmarkResult>
    ) {
        fun toMarkdown(): String {
            val sb = StringBuilder()
            sb.appendLine("## Benchmark Suite: $name")
            sb.appendLine()
            results.forEach { sb.appendLine(it.toMarkdown()) }
            return sb.toString()
        }

        fun toSummaryTable(): String {
            val sb = StringBuilder()
            sb.appendLine("| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |")
            sb.appendLine("|-----------|------|--------|---------|-----|-----|------------|")
            results.forEach { r ->
                sb.appendLine("| ${r.name} | ${formatTime(r.mean)} | ${formatTime(r.median)} | ${formatTime(r.stdDev)} | ${formatTime(r.min.toDouble())} | ${formatTime(r.max.toDouble())} | ${"%.2f".format(r.throughput)} ops/s |")
            }
            return sb.toString()
        }

        private fun formatTime(nanos: Double): String {
            return when {
                nanos < 1000 -> "${"%.0f".format(nanos)}ns"
                nanos < 1_000_000 -> "${"%.0f".format(nanos / 1000)}μs"
                nanos < 1_000_000_000 -> "${"%.0f".format(nanos / 1_000_000)}ms"
                else -> "${"%.2f".format(nanos / 1_000_000_000)}s"
            }
        }
    }

    fun runBenchmark(name: String, code: String): BenchmarkResult {
        if (verbose) println("Running benchmark: $name")

        // Warmup phase
        val warmupTime = measureTimeMillis {
            repeat(warmupIterations) {
                runOnce(code)
            }
        }

        if (verbose) println("  Warmup completed in ${warmupTime}ms")

        // Force GC before measurements
        System.gc()
        Thread.sleep(100)

        val memoryBefore = if (trackMemory) getUsedMemory() else null

        // Measurement phase
        val measurements = mutableListOf<Long>()
        repeat(measurementIterations) {
            measurements.add(measureNanoTime {
                runOnce(code)
            })
        }

        val memoryAfter = if (trackMemory) getUsedMemory() else null

        // Calculate statistics
        val sorted = measurements.sorted()
        val mean = measurements.average()
        val median = if (measurements.size % 2 == 0) {
            (sorted[measurements.size / 2 - 1] + sorted[measurements.size / 2]) / 2.0
        } else {
            sorted[measurements.size / 2].toDouble()
        }

        val variance = measurements.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        val throughput = 1_000_000_000.0 / mean // ops per second

        if (verbose) {
            println("  Measurements completed:")
            println("    Mean: ${mean / 1_000_000}ms")
            println("    Throughput: ${"%.2f".format(throughput)} ops/s")
        }

        return BenchmarkResult(
            name = name,
            warmupTime = warmupTime,
            measurements = measurements,
            mean = mean,
            median = median,
            stdDev = stdDev,
            min = measurements.minOrNull() ?: 0,
            max = measurements.maxOrNull() ?: 0,
            memoryBefore = memoryBefore,
            memoryAfter = memoryAfter,
            throughput = throughput
        )
    }

    fun runBenchmarkFile(file: File): BenchmarkResult {
        val code = file.readText()
        return runBenchmark(file.nameWithoutExtension, code)
    }

    fun runBenchmarkSuite(directory: File): BenchmarkSuite {
        val results = directory.walkTopDown()
            .filter { it.isFile && it.extension == "php" }
            .map { runBenchmarkFile(it) }
            .toList()

        return BenchmarkSuite(directory.name, results)
    }

    private fun runOnce(code: String) {
        val outputStream = ByteArrayOutputStream()
        Context.newBuilder("php")
            .out(outputStream)
            .option("engine.WarnInterpreterOnly", "false")
            .allowAllAccess(true)
            .build().use { context ->
                context.eval(Source.newBuilder("php", code, "benchmark.php").build())
            }
    }

    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val runner = BenchmarkRunner(
                warmupIterations = 10,
                measurementIterations = 50,
                verbose = true
            )

            val benchmarksDir = File("src/test/resources/benchmarks")

            // Run micro benchmarks
            val microDir = File(benchmarksDir, "micro")
            if (microDir.exists()) {
                val microSuite = runner.runBenchmarkSuite(microDir)
                println(microSuite.toMarkdown())
                println("\n## Summary Table\n")
                println(microSuite.toSummaryTable())
            }

            // Run app benchmarks
            val appDir = File(benchmarksDir, "app")
            if (appDir.exists()) {
                val appSuite = runner.runBenchmarkSuite(appDir)
                println(appSuite.toMarkdown())
            }
        }
    }
}
