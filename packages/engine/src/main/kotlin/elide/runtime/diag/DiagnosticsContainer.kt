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
package elide.runtime.diag

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Predicate
import kotlinx.atomicfu.atomic
import elide.runtime.diag.Severity.INFO

/**
 * ## Diagnostics Container
 *
 * Simple container type which holds records complying with [DiagnosticInfo]; functions as both a [DiagnosticsReceiver]
 * and [DiagnosticsBuffer]. This container is thread-safe and can be locked to prevent further writes. Under the hood,
 * it uses a simple [ConcurrentLinkedQueue] and atomics, so it is always safe but not the most performant choice
 * possible.
 *
 * Generally speaking, diagnostics are reported in cases where the engine intends to crash before continuing (compiler
 * errors, warnings in strict mode).
 */
public class DiagnosticsContainer private constructor (initial: Sequence<DiagnosticInfo>? = null) :
  DiagnosticsReceiver, DiagnosticsSuite {
  // Whether this container has locked.
  private val isLocked = atomic(false)

  // Whether this container has closed.
  private val isClosed = atomic(false)

  // Thread-safe held suite of sequences of diagnostics.
  private val held = ConcurrentLinkedQueue<Sequence<DiagnosticInfo>>()

  init {
    if (initial != null) {
      held.add(initial)
    }
  }

  // Thread-safe dirty state map.
  private val dirtyState: MutableMap<String, Boolean> = ConcurrentHashMap()

  // Lock the container if needed, then run the block.
  private inline fun <R> withLocked(crossinline block: () -> R): R {
    isLocked.compareAndSet(expect = false, update = true)
    return block()
  }

  // Fail if the container is locked, otherwise run the block.
  private inline fun <R> withMutable(crossinline block: () -> R): R {
    if (isLocked.value) {
      error("Container is locked.")
    }
    return block()
  }

  override val severity: Severity by lazy {
    withLocked {
      all().maxBy { it.severity.ordinal }.severity
    }
  }

  override val count: UInt by lazy {
    withLocked {
      all().count().toUInt()
    }
  }

  override fun lock() {
    isLocked.value = true
  }

  override fun close() {
    isClosed.value = true
  }

  override fun clear() {
    require(!isLocked.value) { "Cannot clear a locked container" }
    require(!isClosed.value) { "Cannot clear a closed container" }
    dirtyState.clear()
    held.clear()
  }

  override fun report(diag: DiagnosticInfo): Unit = report(sequenceOf(diag))

  override fun report(first: DiagnosticInfo, vararg rest: DiagnosticInfo): Unit = report(sequence {
    yield(first)
    for (diag in rest) {
      yield(diag)
    }
  })

  override fun report(diags: Iterable<DiagnosticInfo>): Unit = report(diags.asSequence())

  override fun report(diags: Sequence<DiagnosticInfo>): Unit = withMutable {
    diags.forEach {
      val key = it.lang ?: ""
      dirtyState[key] = true
    }
    held.add(diags)
  }

  override fun dirty(lang: String?): Boolean = dirtyState[lang ?: ""] ?: false
  override fun all(): Sequence<DiagnosticInfo> = withLocked { held.asSequence().flatten() }

  override fun query(consume: Boolean, criteria: Predicate<DiagnosticInfo>): Sequence<DiagnosticInfo> = all().filter {
    criteria.test(it)
  }

  override fun query(lang: String, tool: String?, consume: Boolean): Sequence<DiagnosticInfo> = query { diag ->
    diag.lang == lang && (tool == null || tool == diag.tool)
  }

  /** Factory methods for diagnostics containers. */
  public companion object {
    /** @return Empty diagnostics container. */
    @JvmStatic public fun empty(): DiagnosticsSuite = object : DiagnosticsSuite {
      override fun clear(): Unit = Unit
      override val count: UInt get() = 0u
      override val severity: Severity get() = INFO
      override fun close(): Unit = Unit
      override fun dirty(lang: String?): Boolean = false
      override fun all(): Sequence<DiagnosticInfo> = emptySequence()
      override fun query(lang: String, tool: String?, consume: Boolean): Sequence<DiagnosticInfo> = emptySequence()
      override fun query(consume: Boolean, criteria: Predicate<DiagnosticInfo>): Sequence<DiagnosticInfo> =
        emptySequence()
    }

    /** @return Empty diagnostics container. */
    @JvmStatic public fun create(): DiagnosticsContainer = DiagnosticsContainer()

    /** @return Diagnostics container from the provided sequence. */
    @JvmStatic public fun from(seq: Sequence<DiagnosticInfo>): DiagnosticsContainer = DiagnosticsContainer(seq)
  }
}
