
## Elide Protocol

This package defines canonical Protocol Buffer sources for use within the Elide framework. These definitions are kept in
sync with identical Flatbuffers and Kotlin definitions, and additionally published to the [Buf Schema Registry][1] for
easy documentation access and dependency management.

### What is the _Elide Protocol_?

Critical framework interactions are defined with well-structured and documented models, so that they can easily be
shared, referenced, documented, and implemented with minimal effort. This includes such functionality as:

- **Assets**: Packaging and metadata for static/frontend assets included in an Elide app's build
- **Metadata**: Application-level metadata used by the framework for build tooling and runtime config
- **Models**: Application-level model annotations and well-known-types
- **RPC**: Substrate for interprocess and inter-server communications
- **VFS**: Virtual file systems managed by the framework on behalf of guest VMs

### How does it work?

Canonical definitions are held in _protocol buffer_ format. These are then generated into `.fbs` ([Flatbuffers][2]
schema definitions), as well as Kotlin sources via the [KotlinX Serialization][3] layer for [Protobuf][4]. This yields
model bindings in pure Kotlin, with use points via the Protocol Buffers runtime library for [Kotlin][5] or [Java][6],
and the [Kotlin][7] bindings for Flatbuffers for performance-critical code.

User models can use Elide annotations to explain their models to the framework. These annotations are only supported in
Protocol Buffers syntax version 3.


[1]: https://buf.build/elide/elide
[2]: https://google.github.io/flatbuffers/
[3]: https://github.com/Kotlin/kotlinx.serialization
[4]: https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md
