package elide.runtime.plugins.api;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;


/**
 * TBD.
 */
public abstract class NativePlugin implements NativePluginAPI {
  private final String id;
  private static final Engine PLUGIN_ENGINE = Engine.newBuilder()
          .allowExperimentalOptions(true)
          .build();

  protected NativePlugin(String pluginId) {
    this.id = pluginId;
  }

  public String getPluginId() {
    return id;
  }

  public void context(Engine engine, Context.Builder builder, String[] args) {
    // (nothing by default)
  }

  @Override
  public void init() {
    // (nothing by default)
  }

  protected void apply(String[] args) {
    context(PLUGIN_ENGINE, initialize(this), args);
  }

  public static Context.Builder initialize(NativePlugin plugin) {
    var builder = Context.newBuilder()
            .allowExperimentalOptions(true)
            .allowNativeAccess(true)
            .engine(PLUGIN_ENGINE);

    plugin.context(PLUGIN_ENGINE, builder, new String[0]);
    return builder;
  }
}
