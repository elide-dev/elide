/**
 * Elide Protocol: Protocol Buffers Implementation
 */
module elide.protocol.protobuf {
  requires java.base;

  requires kotlin.stdlib;

  requires elide.protocol.core;
  requires com.google.protobuf;

  exports tools.elide.base;
  exports tools.elide.app;
  exports tools.elide.call;
  exports tools.elide.call.v1alpha1;
  exports tools.elide.assets;
  exports tools.elide.net;
  exports tools.elide.cli;
  exports tools.elide.crypto;
  exports tools.elide.data;
  exports tools.elide.data.secrets;
  exports tools.elide.db;
  exports tools.elide.http;
  exports tools.elide.kv;
  exports tools.elide.machine;
  exports tools.elide.meta;
  exports tools.elide.model;
  exports tools.elide.page;
  exports tools.elide.std;
  exports tools.elide.struct;
  exports tools.elide.vfs;
}
