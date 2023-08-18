module elide.runtime.core {
  requires elide.graalvm;
  requires org.graalvm.sdk;

  requires kotlin.stdlib;
  requires kotlinx.serialization.core;
  requires kotlinx.serialization.json;
  requires annotations;

  exports elide.runtime.core;
  exports elide.runtime.core.extensions;
  exports elide.runtime.plugins.vfs;
}
