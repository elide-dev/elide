package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import java.time.Duration

/**
 * # GraalVM EE: Sandbox Configuration
 *
 * Configures VM sandbox and resource limit functionality, which is only available in GraalVM Enterprise. These settings
 * are inert on Community Edition.
 *
 * @param maxCpuTime From GraalVM: Limits the total maximum CPU time that (can be) spent running the (guest)
 *   application. No limit is set by default. Example value: '100ms'.
 * @param maxAstDepth From GraalVM: Limits the AST depth of parsed functions. Default: no limit.
 * @param maxHeapMemory From GraalVM: Specifies the maximum heap memory that can be retained by the (guest) application
 *   during its run. No limit is set by default and setting the related expert options has no effect. Example value:
 *   '100MB'.
 * @param maxThreads From GraalVM: Limits the number of threads that can be entered by a context at the same point in
 *   time (default: no limit).
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.enterprise.sandbox")
internal class GuestVMEESandboxConfig(
  var maxCpuTime: Duration = DEFAULT_CPU_TIME,
  var maxAstDepth: Long = DEFAULT_AST_DEPTH,
  var maxHeapMemory: String = DEFAULT_HEAP_MEMORY,
  var maxThreads: Int = DEFAULT_MAX_THREADS,
) {
  internal companion object {
    /** Default maximum CPU time: `1000ms`. */
    val DEFAULT_CPU_TIME: Duration = Duration.ofMillis(1000)

    /** Default maximum AST depth: `-1` (unlimited). */
    const val DEFAULT_AST_DEPTH: Long = -1

    /** Default maximum heap memory: `1gb`. */
    const val DEFAULT_HEAP_MEMORY: String = "1GB"

    /** Default maximum number of threads. */
    const val DEFAULT_MAX_THREADS: Int = -1
  }
}
