module elide.runtime.core {
  requires kotlin.stdlib;
  requires org.graalvm.sdk;
  requires elide.graalvm;
    requires kotlinx.serialization.core;
  requires kotlinx.serialization.json;

  exports elide.runtime.core;
  exports elide.runtime.core.extensions;
  exports elide.runtime.plugins.vfs;
}
