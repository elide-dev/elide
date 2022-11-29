package elide.tool.ssg

import elide.runtime.Logger
import elide.runtime.Logging
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Default static app compiler implementation, which executes the request against the app. */
@Singleton internal class DefaultAppStaticCompiler (
  private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AppStaticCompiler {
  // Private logger.
  private val logging: Logger = Logging.of(DefaultAppStaticCompiler::class)

  // Whether we have prepared this compiler with inputs and parameters.
  private val prepared: AtomicBoolean = AtomicBoolean(false)

  // Whether the compiler is currently executing.
  private val executing: AtomicBoolean = AtomicBoolean(false)

  // Site compiler parameters.
  private lateinit var params: SiteCompilerParams

  // Interpreted application info.
  private lateinit var appInfo: LoadedAppInfo

  // Application loader.
  private lateinit var loader: AppLoader

  // Count of expected requests.
  private val expected: AtomicInteger = AtomicInteger(0)

  // Count of failed requests.
  private val failures: AtomicInteger = AtomicInteger(0)

  // Count of completed requests.
  private val completed: AtomicInteger = AtomicInteger(0)

  // List of jobs to execute. Grows as new jobs are added.
  private val activeJobs: ArrayList<Deferred<StaticFragment?>> = ArrayList()

  // List of completed jobs.
  private val finishedJobs: ArrayList<Deferred<StaticFragment?>> = ArrayList()

  // Set of "discovered" static fragments, based on configured fragments.
  private val discovered: ArrayList<StaticFragmentSpec> = ArrayList()

  // Seen URLs.
  private val seen: ConcurrentSkipListSet<String> = ConcurrentSkipListSet()

  // Reset the state of the compiler.
  private fun reset() {
    expected.set(0)
    completed.set(0)
    failures.set(0)
    activeJobs.clear()
    activeJobs.trimToSize()
    finishedJobs.clear()
    finishedJobs.trimToSize()
    discovered.clear()
    discovered.trimToSize()
  }

  // Execute a request against the app loader, with a way to add discovered requests to the stack.
  @Suppress("UNUSED_PARAMETER") private suspend fun fulfillRequestAsync(
    app: LoadedAppInfo,
    spec: StaticFragmentSpec,
  ): Deferred<StaticFragment?> = withContext(dispatcher) {
    async {
      loader.executeRequest(spec)
    }
  }

  // Indicates whether we are done processing tasks.
  private fun done(): Boolean {
    return expected.get() == completed.get()
  }

  // Build a successful compiler result.
  private fun buildResult(buf: StaticSiteBuffer): SiteCompileResult.Success {
    return SiteCompileResult.Success(
      params,
      appInfo,
      params.output.path,
      buf,
    )
  }

  // Check the provided request for uniqueness and enqueue it if it has not been seen yet.
  private suspend fun checkEnqueueRequest(
    appInfo: LoadedAppInfo,
    spec: StaticFragmentSpec,
  ): Deferred<StaticFragment?>? {
    val target = spec.request.uri.toString()
    return if (!seen.contains(target)) {
      seen.add(target)
      logging.trace("URL '$target' has not been seen; enqueueing for fetch")
      val job = fulfillRequestAsync(
        appInfo,
        spec,
      )
      job
    } else {
      logging.trace("URL '$target' has already been seen; skipping")
      null
    }
  }

  /** @inheritDoc */
  override fun prepare(params: SiteCompilerParams, appInfo: LoadedAppInfo, loader: AppLoader) {
    this.params = params
    this.appInfo = appInfo
    this.loader = loader
    this.prepared.compareAndSet(false, true)
  }

  /** @inheritDoc */
  override fun close() {
    executing.set(false)
    reset()
  }

  /** @inheritDoc */
  override suspend fun compileStaticSiteAsync(
    count: Int,
    appInfo: LoadedAppInfo,
    seed: Sequence<StaticFragmentSpec>,
    buffer: StaticSiteBuffer,
  ): Deferred<SiteCompileResult> = withContext(dispatcher) {
    // must have parameters set
    require(prepared.get()) {
      "Must prepare compiler implementation before calling `compileStaticSite`."
    }
    require(!executing.get()) {
      "Cannot execute static site compiler while it is already executing."
    }
    reset()
    executing.compareAndSet(false, true)

    // fill in stateful parameters
    return@withContext async {
      expected.set(count)
      activeJobs.ensureCapacity(count)

      // start initial set of tasks
      for (spec in seed) {
        val job = checkEnqueueRequest(
          appInfo,
          spec,
        )
        if (job != null)
          activeJobs.add(job)
      }

      while (!done()) {
        // wait for all current jobs to complete
        activeJobs.joinAll()
        val batch = ArrayList(activeJobs)
        activeJobs.clear()

        // consume results
        for (job in batch) {
          val fragment = job.await()
          completed.incrementAndGet()
          if (fragment == null) {
            failures.incrementAndGet()
            continue  // skip: job failed (it will be present in the log already
          }

          buffer.add(fragment)
          if (fragment.discovered.isNotEmpty()) {
            // add to expected jobs
            expected.addAndGet(fragment.discovered.size)
            discovered.addAll(fragment.discovered)

            // spawn job for each discovered fragment
            for (spec in fragment.discovered) {
              val subjob = checkEnqueueRequest(
                appInfo,
                spec,
              )
              if (subjob != null) {
                activeJobs.add(subjob)
              }
            }
          }

          // add to finished jobs
          finishedJobs.add(job)
        }
      }

      // no longer executing
      executing.compareAndSet(true, false)
      buildResult(buffer)
    }
  }
}
