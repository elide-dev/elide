/**
 * Elide Protocol: KotlinX Implementation
 */
module elide.protocol.kotlinx {
  requires java.base;
  requires kotlin.stdlib;
  requires kotlinx.serialization.core;
  requires kotlinx.serialization.protobuf;

  requires elide.protocol.core;
}
