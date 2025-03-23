#!/usr/bin/env bun

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

/**
 * Log Analyzer Tool
 *
 * This tool analyzes log files to identify the costliest operations
 * based on wall-clock time differences between log lines.
 */

// Define interface for a parsed log entry
interface LogEntry {
  lineNumber: number
  rawLine: string
  relativeMs: number | null // null for continuation lines
  isStartOfEntry: boolean
}

// Define interface for time jump information
interface TimeJump {
  fromLineIndex: number
  toLineIndex: number
  fromMs: number
  toMs: number
  timeDifference: number
}

// Main function to analyze logs
async function analyzeLogFile(filePath: string, contextLines: number = 5, minJumpMs: number = 10) {
  try {
    console.log(`Analyzing log ${filePath === "-" ? "from stdin" : `file: ${filePath}`}`)
    console.log(`Context lines: ${contextLines}, Minimum jump threshold: ${minJumpMs}ms`)

    // Read from stdin or file
    let fileContent: string
    if (filePath === "-") {
      fileContent = await readFromStdin()
    } else {
      fileContent = await Bun.file(filePath).text()
    }
    const lines = fileContent.split("\n")

    console.log(`Read ${lines.length} lines from file`)

    // Parse log entries
    const logLines: LogEntry[] = []
    let validTimestampCount = 0

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i]
      const entry = parseLogLine(line, i + 1)
      logLines.push(entry)

      if (entry.relativeMs !== null) {
        validTimestampCount++
      }
    }

    console.log(`Found ${validTimestampCount} lines with valid timestamps`)

    if (validTimestampCount === 0) {
      console.error("No valid timestamps found in the log file. Please check the format.")
      return
    }

    // Calculate time jumps
    const timeJumps = calculateTimeJumps(logLines, minJumpMs)

    // Sort time jumps by time difference (descending)
    timeJumps.sort((a, b) => b.timeDifference - a.timeDifference)

    // Calculate total log time
    let firstTimestamp = -1
    let lastTimestamp = -1

    for (const line of logLines) {
      if (line.relativeMs !== null) {
        if (firstTimestamp === -1) {
          firstTimestamp = line.relativeMs
        }
        lastTimestamp = line.relativeMs
      }
    }

    const totalLogTimeMs = lastTimestamp - firstTimestamp

    // Output results
    console.log(`\n${bold(cyan("Log Analysis Summary:"))}`)
    console.log(dim(`--------------------------`))
    console.log(`Total log duration: ${bold(cyan(totalLogTimeMs + "ms"))}`)
    console.log(`Found ${bold(timeJumps.length.toString())} time jumps >= ${minJumpMs}ms`)

    if (timeJumps.length > 0) {
      // Calculate sum of top jumps
      const topJumps = timeJumps.slice(0, Math.min(10, timeJumps.length))
      const sumTopJumps = topJumps.reduce((sum, jump) => sum + jump.timeDifference, 0)

      const percentOfTotal = ((sumTopJumps / totalLogTimeMs) * 100).toFixed(2)
      console.log(
        `Top ${topJumps.length} jumps account for ${bold(red(sumTopJumps + "ms"))} (${bold(percentOfTotal)}% of total log time)`,
      )
      console.log(`\n${bold("Top time jumps with context:")}`)
      console.log(dim(`--------------------------`))

      // Output top jumps with context
      for (let i = 0; i < topJumps.length; i++) {
        const jump = topJumps[i]

        // Use color intensity based on jump size
        const jumpColor = getHeatColor(jump.timeDifference, topJumps[0].timeDifference)

        console.log(
          `${bold(`Jump #${i + 1}:`)} ${colorize(jumpColor, bold(jump.timeDifference + "ms"))} between lines ${logLines[jump.fromLineIndex].lineNumber} and ${logLines[jump.toLineIndex].lineNumber}`,
        )
        console.log(
          `Time span: ${jump.fromMs}ms → ${jump.toMs}ms (${bold(((jump.timeDifference / totalLogTimeMs) * 100).toFixed(2) + "%")} of total log time)`,
        )

        // Get context lines
        const contextStart = Math.max(0, jump.fromLineIndex - contextLines)
        const contextEnd = Math.min(logLines.length - 1, jump.toLineIndex + contextLines)

        console.log(dim(`--------------------------`))

        // Find the longest line number for alignment
        const maxLineNumberLength = String(logLines[contextEnd].lineNumber).length

        // Output context
        for (let j = contextStart; j <= contextEnd; j++) {
          const entry = logLines[j]
          const lineNumber = String(entry.lineNumber).padStart(maxLineNumberLength, " ")

          let line: string
          let lineColor: string

          // Determine line color and formatting based on position
          if (j === jump.fromLineIndex) {
            // FROM line - colorize based on severity
            lineColor = yellow
            const timeInfo = entry.relativeMs !== null ? ` [${entry.relativeMs}ms]` : ""
            line = `${bold(yellow("FROM"))} ${dim(lineNumber)} │ ${entry.rawLine}${dim(timeInfo)}`
          } else if (j === jump.toLineIndex) {
            // TO line - colorize in hot color
            lineColor = jumpColor
            const timeInfo = entry.relativeMs !== null ? ` [${entry.relativeMs}ms]` : ""
            const timeDiff = ` +${jump.timeDifference}ms`
            line = `${bold(colorize(jumpColor, "TO  "))} ${dim(lineNumber)} │ ${entry.rawLine}${dim(timeInfo)}${bold(colorize(jumpColor, timeDiff))}`
          } else {
            // Context line - dim and use cold colors
            lineColor = dim
            line = `     ${dim(lineNumber)} │ ${dim(entry.rawLine)}`
          }

          console.log(line)
        }

        console.log(dim(`--------------------------`))
      }
    } else {
      console.log(`${yellow("No significant time jumps found.")}`)
    }
  } catch (error) {
    console.error("Error analyzing log file:", error)
    console.error(error)
  }
}

// Parse a single log line
function parseLogLine(line: string, lineNumber: number): LogEntry {
  // Remove ANSI color codes and other formatting
  const cleanLine = removeFormattingCharacters(line)

  // Try to match a line with relative_ms timestamp
  // This regex looks for either "elide" followed by a number, or just a number at the start
  const regex = /^(?:elide\s+)?(\d+)\s+/
  const match = cleanLine.match(regex)

  if (match) {
    return {
      lineNumber,
      rawLine: line,
      relativeMs: parseInt(match[1], 10),
      isStartOfEntry: true,
    }
  }

  // If no match, this is a continuation line
  return {
    lineNumber,
    rawLine: line,
    relativeMs: null,
    isStartOfEntry: false,
  }
}

// Remove ANSI color codes and other formatting characters
function removeFormattingCharacters(text: string): string {
  // This regex removes ANSI escape codes for colors and formatting
  return text.replace(/\u001b\[\d+(;\d+)*m/g, "")
}

// Calculate time jumps between consecutive log entries with timestamps
function calculateTimeJumps(logLines: LogEntry[], minJumpMs: number): TimeJump[] {
  const timeJumps: TimeJump[] = []
  let lastTimestampIndex = -1

  for (let i = 0; i < logLines.length; i++) {
    const line = logLines[i]

    if (line.relativeMs !== null) {
      if (lastTimestampIndex !== -1) {
        const lastLine = logLines[lastTimestampIndex]
        // This is correct: we're calculating wall-clock time differences between log events
        // relative_ms represents time since application start, so the difference is the
        // actual time elapsed between these two log events
        const timeDifference = line.relativeMs - lastLine.relativeMs!

        // Only consider time differences above the minimum threshold
        if (timeDifference >= minJumpMs) {
          timeJumps.push({
            fromLineIndex: lastTimestampIndex,
            toLineIndex: i,
            fromMs: lastLine.relativeMs!,
            toMs: line.relativeMs,
            timeDifference,
          })
        }
      }

      lastTimestampIndex = i
    }
  }

  return timeJumps
}

// Helper functions for ANSI color formatting
function bold(text: string): string {
  return `\x1b[1m${text}\x1b[0m`
}

function dim(text: string): string {
  return `\x1b[2m${text}\x1b[0m`
}

// Color functions
const red = (text: string): string => `\x1b[31m${text}\x1b[0m`
const green = (text: string): string => `\x1b[32m${text}\x1b[0m`
const yellow = (text: string): string => `\x1b[33m${text}\x1b[0m`
const blue = (text: string): string => `\x1b[34m${text}\x1b[0m`
const magenta = (text: string): string => `\x1b[35m${text}\x1b[0m`
const cyan = (text: string): string => `\x1b[36m${text}\x1b[0m`
const gray = (text: string): string => `\x1b[90m${text}\x1b[0m`

// Generic color function
function colorize(colorCode: string, text: string): string {
  return `${colorCode}${text}\x1b[0m`
}

// Get heat color based on time difference (red for highest, yellow for medium, etc.)
function getHeatColor(timeDiff: number, maxTimeDiff: number): string {
  const percentage = timeDiff / maxTimeDiff

  if (percentage >= 0.8) return "\x1b[31m" // Red (hot)
  if (percentage >= 0.5) return "\x1b[33m" // Yellow (warm)
  if (percentage >= 0.3) return "\x1b[32m" // Green (medium)
  return "\x1b[36m" // Cyan (cool)
}

// Read data from stdin
async function readFromStdin(): Promise<string> {
  const chunks: Uint8Array[] = []

  for await (const chunk of Bun.stdin.stream()) {
    chunks.push(chunk)
  }

  return Buffer.concat(chunks).toString()
}

// Parse command line arguments
function parseArgs() {
  const args = process.argv.slice(2)
  let filePath: string | undefined
  let contextLines = 5
  let minJumpMs = 10

  for (let i = 0; i < args.length; i++) {
    if (args[i] === "--context" || args[i] === "-c") {
      contextLines = parseInt(args[++i], 10)
    } else if (args[i] === "--min-jump" || args[i] === "-m") {
      minJumpMs = parseInt(args[++i], 10)
    } else if (args[i] === "--help" || args[i] === "-h") {
      showHelp()
      process.exit(0)
    } else if (!filePath) {
      filePath = args[i]
    }
  }

  return { filePath, contextLines, minJumpMs }
}

function showHelp() {
  console.log(`Log Analyzer - Find costly operations in logs`)
  console.log(`\nUsage: log-analyzer.ts <file|-> [options]`)
  console.log(`\nArguments:`)
  console.log(`  <file>                    Path to log file, or '-' to read from stdin`)
  console.log(`\nOptions:`)
  console.log(`  --context, -c <lines>     Number of context lines to show (default: 5)`)
  console.log(`  --min-jump, -m <ms>       Minimum time jump to consider in ms (default: 10)`)
  console.log(`  --help, -h                Show this help message`)
}

// Main execution
const { filePath, contextLines, minJumpMs } = parseArgs()

if (!filePath) {
  console.error("Please provide a path to the log file or '-' to read from stdin")
  showHelp()
  process.exit(1)
}

analyzeLogFile(filePath, contextLines, minJumpMs)
