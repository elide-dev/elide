module elide.runtime.core {
  requires kotlin.stdlib;
  requires org.graalvm.sdk;
  requires elide.graalvm;

  exports elide.runtime.core;
  exports elide.runtime.core.extensions;
  exports elide.runtime.plugins.vfs;
}
