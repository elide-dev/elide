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

@file:Suppress("ImplicitDefaultLocale", "MagicNumber")

package elide.tooling.reporting.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

/**
 * HTML template builder for test reports using kotlinx-html DSL.
 */
internal class HtmlTemplateBuilder {
  
  /**
   * Generate complete HTML report from test data.
   */
  fun generateReport(
    summary: HtmlTestSummary,
    testHierarchy: HtmlTestGroup
  ): String = createHTML().html {
    lang = "en"
    
    head {
      meta(charset = "UTF-8")
      meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
      title("Test Report - ${summary.suiteName}")
      style {
        unsafe { +HtmlAssets.css }
      }
    }
    
    body {
      div(classes = "container") {
        renderHeader(summary)
        renderSummaryCards(summary)
        renderControls()
        renderTestResults(testHierarchy)
      }
      
      script {
        unsafe { +HtmlAssets.javascript }
      }
    }
  }
  
  private fun DIV.renderHeader(summary: HtmlTestSummary) {
    div(classes = "header") {
      h1 { text("Test Report: ${summary.suiteName}") }
      div(classes = "timestamp") {
        text("Generated on ${summary.timestamp.atZone(java.time.ZoneId.systemDefault())
          .format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm:ss"))}")
      }
    }
  }
  
  private fun DIV.renderSummaryCards(summary: HtmlTestSummary) {
    div(classes = "summary") {
      div(classes = "summary-bar") {
        div(classes = "summary-group test-counts") {
          div(classes = "summary-item total") {
            span(classes = "summary-label") { text("Total:") }
            span(classes = "summary-value") { text("${summary.totalTests}") }
          }
          div(classes = "summary-item passed") {
            span(classes = "summary-label") { text("Passed:") }
            span(classes = "summary-value") { text("${summary.passed}") }
          }
          div(classes = "summary-item failed") {
            span(classes = "summary-label") { text("Failed:") }
            span(classes = "summary-value") { text("${summary.failed}") }
          }
          div(classes = "summary-item skipped") {
            span(classes = "summary-label") { text("Skipped:") }
            span(classes = "summary-value") { text("${summary.skipped}") }
          }
          div(classes = "summary-item pass-rate") {
            span(classes = "summary-label") { text("Pass Rate:") }
            span(classes = "summary-value") { text("${String.format("%.1f", summary.passRate * 100)}%") }
          }
        }
        div(classes = "summary-group timing") {
          div(classes = "summary-item duration") {
            span(classes = "summary-label") { text("Duration:") }
            span(classes = "summary-value") { text(formatDuration(summary.duration)) }
          }
        }
      }
    }
  }
  
  private fun DIV.renderControls() {
    div(classes = "controls") {
      input(type = InputType.text, classes = "search-box") {
        id = "search"
        placeholder = "Search tests..."
      }
      
      div(classes = "filter-buttons") {
        button(classes = "filter-btn all active") {
          attributes["data-filter"] = "all"
          text("All")
        }
        button(classes = "filter-btn passed") {
          attributes["data-filter"] = "passed"
          text("Passed")
        }
        button(classes = "filter-btn failed") {
          attributes["data-filter"] = "failed"
          text("Failed")
        }
        button(classes = "filter-btn skipped") {
          attributes["data-filter"] = "skipped"
          text("Skipped")
        }
      }
      
      div(classes = "expand-controls") {
        button(classes = "expand-btn") {
          id = "expand-all"
          text("Expand All")
        }
        button(classes = "expand-btn") {
          id = "collapse-all"
          text("Collapse All")
        }
      }
    }
  }
  
  private fun DIV.renderTestResults(rootGroup: HtmlTestGroup) {
    div(classes = "test-results") {
      // If root group has direct tests, render them
      if (rootGroup.tests.isNotEmpty()) {
        renderTestGroup("Root Tests", rootGroup.tests, emptyList(), rootGroup.summary)
      }
      
      // Render child groups
      rootGroup.children.forEach { group ->
        renderTestGroupRecursive(group)
      }
    }
  }
  
  private fun DIV.renderTestGroupRecursive(group: HtmlTestGroup) {
    div(classes = "test-group") {
      // Group header
      div(classes = "group-header") {
        div(classes = "group-title") {
          span(classes = "group-toggle") { text("▼") }
          text(group.name)
        }
        div(classes = "group-stats") {
          if (group.summary.passed > 0) {
            div(classes = "stat") {
              div(classes = "stat-dot passed")
              text("${group.summary.passed} passed")
            }
          }
          if (group.summary.failed > 0) {
            div(classes = "stat") {
              div(classes = "stat-dot failed")
              text("${group.summary.failed} failed")
            }
          }
          if (group.summary.skipped > 0) {
            div(classes = "stat") {
              div(classes = "stat-dot skipped")
              text("${group.summary.skipped} skipped")
            }
          }
        }
      }
      
      // Group content
      div(classes = "group-content") {
        // Render direct tests
        group.tests.forEach { test ->
          renderTestCase(test)
        }
        
        // Render child groups
        group.children.forEach { childGroup ->
          renderTestGroupRecursive(childGroup)
        }
      }
    }
  }
  
  private fun DIV.renderTestGroup(
    groupName: String,
    tests: List<HtmlTestCase>,
    @Suppress("UNUSED_PARAMETER") path: List<String>,
    summary: HtmlGroupSummary
  ) {
    div(classes = "test-group") {
      // Group header
      div(classes = "group-header") {
        div(classes = "group-title") {
          span(classes = "group-toggle") { text("▼") }
          text(groupName)
        }
        div(classes = "group-stats") {
          if (summary.passed > 0) {
            div(classes = "stat") {
              div(classes = "stat-dot passed")
              text("${summary.passed} passed")
            }
          }
          if (summary.failed > 0) {
            div(classes = "stat") {
              div(classes = "stat-dot failed")
              text("${summary.failed} failed")
            }
          }
          if (summary.skipped > 0) {
            div(classes = "stat") {
              div(classes = "stat-dot skipped")
              text("${summary.skipped} skipped")
            }
          }
        }
      }
      
      // Group content
      div(classes = "group-content") {
        tests.forEach { test ->
          renderTestCase(test)
        }
      }
    }
  }
  
  private fun DIV.renderTestCase(test: HtmlTestCase) {
    div(classes = "test-case") {
      attributes["data-status"] = test.status.name.lowercase()
      
      div(classes = "test-header") {
        div(classes = "test-info") {
          div(classes = "test-status-indicator ${test.status.cssClass}") {
            text(when(test.status) {
              HtmlTestStatus.PASSED -> "✓"
              HtmlTestStatus.FAILED -> "✗"
              HtmlTestStatus.SKIPPED -> "⚬"
            })
          }
          div(classes = "test-name-info") {
            div(classes = "test-name") { text(test.name) }
            div(classes = "test-meta") {
              text("${test.className}")
              if (test.parentPath.isNotEmpty()) {
                text(" • ${test.parentPath.joinToString(" > ")}")
              }
            }
          }
        }
        div(classes = "test-duration") {
          text(formatDuration(test.duration))
        }
      }
      
      // Render failure/skip details (initially hidden)
      if (test.status == HtmlTestStatus.FAILED || test.status == HtmlTestStatus.SKIPPED) {
        div(classes = "test-details") {
          if (test.status == HtmlTestStatus.FAILED) {
            test.errorMessage?.let { message ->
              div(classes = "error-message") { text(message) }
            }
            test.stackTrace?.let { stackTrace ->
              div(classes = "stack-trace") { text(stackTrace) }
            }
          }
          
          if (test.status == HtmlTestStatus.SKIPPED && test.skipReason != null) {
            div(classes = "skip-reason") {
              text("Skipped: ${test.skipReason}")
            }
          }
        }
      }
    }
  }
  
  /**
   * Format duration for display.
   */
  private fun formatDuration(duration: Duration): String {
    val seconds = duration.inWholeMilliseconds / 1000.0
    return when {
      seconds < 1.0 -> "${duration.inWholeMilliseconds}ms"
      seconds < 60.0 -> String.format("%.2fs", seconds)
      else -> {
        val minutes = (seconds / 60).toInt()
        val remainingSeconds = seconds % 60
        "${minutes}m ${String.format("%.1f", remainingSeconds)}s"
      }
    }
  }
}
