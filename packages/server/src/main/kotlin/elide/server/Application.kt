package elide.server

import elide.server.runtime.jvm.SecurityProviderConfigurator
import elide.server.util.ServerFlag
import io.micronaut.runtime.Micronaut

/**
 * Static class interface which equips a Micronaut application with extra initialization features powered by Elide; to
 * use, simply enforce that your entrypoint object complies with this interface.
 */
public interface Application {
  /**
   * Boot an Elide application with the provided [args], if any.
   *
   * Elide parses its own arguments and applies configuration or state based on any encountered values. All Elide flags
   * are prefixed with "--elide.". Micronaut-relevant arguments are passed on to Micronaut, and user args are
   * additionally made available.
   *
   * Elide server arguments can be interrogated via [ServerFlag]s.
   *
   * @param args Arguments passed to the application.
   */
  @Suppress("SpreadOperator")
  public fun boot(args: Array<String>) {
    SecurityProviderConfigurator.initialize()
    ServerFlag.setArgs(args)
    Micronaut.build().start()
  }
}
