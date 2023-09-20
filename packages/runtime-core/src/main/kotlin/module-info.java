module elide.runtime.core {
  requires elide.graalvm;
  requires org.graalvm.polyglot;

  requires kotlin.stdlib;
  requires kotlinx.serialization.core;
  requires kotlinx.serialization.json;

  exports elide.runtime.core;
  exports elide.runtime.core.extensions;
  exports elide.runtime.plugins;
  exports elide.runtime.plugins.vfs;
}
