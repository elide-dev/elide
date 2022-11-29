package elide.tool.ssg

import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope as coroutine
import java.io.Closeable
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Class which holds output fragments and state as they are built within the SSG compiler. */
public class StaticSiteBuffer : Closeable, AutoCloseable {
  // Count of fragments held by the buffer.
  private val fragmentCount: AtomicInteger = AtomicInteger(0)

  // Marked as open until fragments are done, then closed.
  private val open: AtomicBoolean = AtomicBoolean(true)

  // Marked as closed when all fragments and consumption have completed.
  private val closed: AtomicBoolean = AtomicBoolean(false)

  // Whether we are currently iterating over results.
  private val consuming: AtomicBoolean = AtomicBoolean(false)

  // All registered fragments.
  private val allFragments: Queue<StaticFragment> = ConcurrentLinkedQueue()

  /** @inheritDoc */
  override fun close() {
    closed.compareAndSet(false, true)
  }

  // Reset buffer state.
  @VisibleForTesting internal fun reset() {
    allFragments.clear()
    fragmentCount.set(0)
    open.set(true)
    closed.set(false)
  }

  /**
   * Let the buffer know that all pending fragments have completed, and that this current set of fragments constitutes
   * the full set of results.
   *
   * After this call, fragments cannot be added to the buffer; it is marked as closed, which also opens up consumption
   * of the fragments via an iterable.
   *
   * @throws IllegalStateException if the buffer has already been sealed.
   */
  internal fun seal() {
    open.compareAndSet(true, false)
  }

  /** @return Indicate whether the buffer is open. */
  internal fun isOpen(): Boolean = open.get() && !closed.get()

  /** @return Indicate whether the buffer is closed. */
  internal fun isClosed(): Boolean = !open.get() && closed.get()

  /**
   * Add a compiled [StaticFragment] to the current set of buffered fragments.
   *
   * @param fragment Compiled site fragment to add.
   * @return Current count of fragments so far.
   * @throws IllegalStateException if the buffer is sealed.
   */
  internal fun add(fragment: StaticFragment): Int {
    check(open.get() && !closed.get()) {
      "Output buffer must be in an open state to accept fragments"
    }
    allFragments.add(fragment)
    return fragmentCount.incrementAndGet()
  }

  /** @return Size of the set of fragments in the buffer. */
  public fun size(): Int = fragmentCount.get()

  /**
   * Consume each fragment within the set of [allFragments]; can only be called before the buffer is closed.
   *
   * @return A sequence of all fragments within the buffer.
   * @throws IllegalStateException if the buffer has not yet been sealed, or is closed.
   */
  public suspend fun <R: Any> consumeAsync(consumer: suspend (StaticFragment) -> R): List<Deferred<R>> = coroutine {
    check(!open.get() && !closed.get()) {
      "Cannot consume from output buffer while it is open for additional fragments"
    }
    check(!consuming.getAndSet(true)) {
      "Cannot consume from output buffer while it is already being consumed"
    }

    var frag = allFragments.poll()
    val jobs: ArrayList<Deferred<R>> = ArrayList()
    while (frag != null) {
      // consume the fragment
      val current = frag
      jobs.add(async {
        consumer.invoke(current)
      })
      frag = allFragments.poll()
    }
    consuming.set(false)
    jobs
  }
}
