module elide.runtime.core {
  requires elide.base;
  requires elide.graalvm;
  requires elide.base;

  requires org.graalvm.polyglot;

  requires kotlin.stdlib;
  requires kotlinx.serialization.core;
  requires kotlinx.serialization.json;

  requires io.netty.codec.http;
  requires io.netty.transport;
  requires io.netty.buffer;
  requires io.netty.transport.classes.epoll;
  requires io.netty.transport.classes.kqueue;
  requires io.netty.incubator.transport.classes.io_uring;

  exports elide.runtime.core;
  exports elide.runtime.core.extensions;
  exports elide.runtime.plugins;
  exports elide.runtime.plugins.vfs;
}
