//[base](../../../index.md)/[elide.model](../index.md)/[AppModel](index.md)/[toMessage](to-message.md)

# toMessage

[common, jvm, native]\
[common]\
expect abstract fun [toMessage](to-message.md)(): [M](index.md)

[jvm, native]\
actual abstract fun [toMessage](to-message.md)(): [M](index.md)

Translate the current [AppModel](index.md) into an equivalent [WireMessage](../-wire-message/index.md) instance [M](index.md).

#### Return

Message instance corresponding to this model.

[js]\
actual abstract fun [toMessage](to-message.md)(): [M](index.md)

Translate the current [AppModel](index.md) into an equivalent [WireMessage](../-wire-message/index.md) instance [M](index.md).

#### Return

lib.protobuf.Message instance corresponding to this model.
