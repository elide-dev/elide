package elide.runtime.plugins.api;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;


/**
 * TBD.
 */
public interface NativePluginAPI {
  public String getPluginId();
  public void context(Engine engine, Context.Builder builder, String[] args);
  public void init();
}
