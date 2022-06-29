//[model](../../../index.md)/[elide.model](../index.md)/[AppModel](index.md)

# AppModel

[common]\
expect interface [AppModel](index.md)&lt;[M](index.md) : [WireMessage](../-wire-message/index.md)&gt;

Describes the expected interface for model objects which are reliably serializable into [WireMessage](../-wire-message/index.md) instances.

[js, jvm, native]\
actual interface [AppModel](index.md)&lt;[M](index.md) : [WireMessage](../-wire-message/index.md)&gt;

Describes the expected interface for model objects which are reliably serializable into [WireMessage](../-wire-message/index.md) instances.

## Functions

| Name | Summary |
|---|---|
| [toMessage](to-message.md) | [common, js, jvm, native]<br>[common]<br>expect abstract fun [toMessage](to-message.md)(): [M](index.md)<br>[js, jvm, native]<br>actual abstract fun [toMessage](to-message.md)(): [M](index.md)<br>Translate the current [AppModel](index.md) into an equivalent [WireMessage](../-wire-message/index.md) instance [M](index.md). |
