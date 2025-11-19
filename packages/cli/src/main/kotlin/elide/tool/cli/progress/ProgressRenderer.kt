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
package elide.tool.cli.progress

import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.*
import com.github.ajalt.mordant.widgets.ProgressBar

/**
 * Tool for rendering a [ProgressState] into a [Widget].
 *
 * @author Lauri Heino <datafox>
 */
internal object ProgressRenderer {
  fun render(state: ProgressState): Widget {
    return table {
      column(0) { width = ColumnWidth.Expand(1) }
      column(1) { width = ColumnWidth.Expand(1) }
      borderType = BorderType.SQUARE
      header { header(state) }
      body {
        val notStarted = state.tasks.filter { !it.started }
        val running = state.tasks.filter { it.started && !it.finished }
        val completed = state.tasks.filter { it.finished }
        if (notStarted.isNotEmpty()) row { cell(tasks(notStarted)) { columnSpan = 2 } }
        running.forEach { task ->
          row {
            cell(task(task))
            cell(console(task))
          }
        }
        if (completed.isNotEmpty()) row { cell(tasks(completed)) { columnSpan = 2 } }
      }
    }
  }

  private fun SectionBuilder.header(state: ProgressState) {
    if (state.tasks.all { !it.started } || state.tasks.all { it.finished }) {
      row { cell(state.name) { columnSpan = 2 } }
      return
    }
    row {
      cell(state.name) { columnSpan = 2 }
      cellBorders = Borders.LEFT_TOP_RIGHT
    }
    row {
      cellBorders = Borders.LEFT_RIGHT_BOTTOM
      val total = state.tasks.sumOf { task -> task.target }.toLong()
      val completed = state.tasks.sumOf { task -> task.position.coerceAtLeast(0) }.toLong()
      cell(progressBar(total, completed, state.tasks.any { it.failed })) { columnSpan = 2 }
    }
  }

  private fun progressBar(total: Long, completed: Long, failed: Boolean): Any? {
    val style = if (failed) TextStyle(TextColors.red) else null
    return if (total == 0L && completed == 1L) ProgressBar(indeterminate = true, indeterminateStyle = style)
    else ProgressBar(total, completed, completeStyle = style, finishedStyle = style)
  }

  private fun tasks(tasks: List<TrackedTask>): Table {
    return table {
      tableBorders = Borders.NONE
      cellBorders = Borders.NONE
      padding { all = 0 }
      body { tasks.forEach { task -> row { title(task) } } }
    }
  }

  private fun RowBuilder.title(task: TrackedTask) {
    val sb = StringBuilder()
    sb.append(task.name).append(": ").append(task.state.displayName)
    if (task.status.isNotBlank()) sb.append(" (").append(task.status).append(")")
    cell(sb.toString()) { if (task.failed) style(TextColors.red) }
  }

  private fun task(task: TrackedTask): Table {
    return table {
      tableBorders = Borders.NONE
      cellBorders = Borders.NONE
      padding { all = 0 }
      header { row { title(task) } }
      body {
        if (task.started && !task.finished) {
          row { cell(progressBar(task.target.toLong(), task.position.toLong(), task.failed)) }
        }
      }
    }
  }

  private fun console(task: TrackedTask): Table {
    return table {
      tableBorders = Borders.NONE
      cellBorders = Borders.NONE
      padding { all = 0 }
      body { task.output.toSortedMap().values.lastElements(2).forEach { output -> row { cell(output) } } }
    }
  }

  private fun <T> Collection<T>.lastElements(count: Int): List<T> {
    if (size <= count) return toList()
    return toList().subList(size - count, size)
  }
}
