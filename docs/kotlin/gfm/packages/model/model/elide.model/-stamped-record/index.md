//[model](../../../index.md)/[elide.model](../index.md)/[StampedRecord](index.md)

# StampedRecord

[common]\
expect interface [StampedRecord](index.md)&lt;[K](index.md), [M](index.md) : [WireMessage](../-wire-message/index.md)&gt; : [AppRecord](../-app-record/index.md)&lt;[K](index.md), [M](index.md)&gt; 

Describes the expected interface for model records which carry designated create/update timestamps.

Stamped records extend the base [AppRecord](../-app-record/index.md) interface with the [createdAt](created-at.md) and [updatedAt](updated-at.md) timestamp fields. These fields are typically provided by the database or the application runtime, and don't need to be set explicitly by the developer, although explicitly set values do override automatic values.

[js, jvm, native]\
actual interface [StampedRecord](index.md)&lt;[K](index.md), [M](index.md) : [WireMessage](../-wire-message/index.md)&gt; : [AppRecord](../-app-record/index.md)&lt;[K](index.md), [M](index.md)&gt; 

Describes the expected interface for model records which carry designated create/update timestamps.

Stamped records extend the base [AppRecord](../-app-record/index.md) interface with the [createdAt](created-at.md) and [updatedAt](updated-at.md) timestamp fields. These fields are typically provided by the database or the application runtime, and don't need to be set explicitly by the developer, although explicitly set values do override automatic values.

## Functions

| Name | Summary |
|---|---|
| [createdAt](created-at.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [createdAt](created-at.md)(): Instant?<br>[js, jvm, native]<br>actual open fun [createdAt](created-at.md)(): Instant? |
| [displayName](../-app-record/display-name.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [displayName](../-app-record/display-name.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>[js, jvm, native]<br>actual open fun [displayName](../-app-record/display-name.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [id](../-app-record/id.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [id](../-app-record/id.md)(): [K](index.md)?<br>[js, jvm, native]<br>actual open fun [id](../-app-record/id.md)(): [K](index.md)? |
| [parentId](../-app-record/parent-id.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [parentId](../-app-record/parent-id.md)(): [K](index.md)?<br>[js, jvm, native]<br>actual open fun [parentId](../-app-record/parent-id.md)(): [K](index.md)? |
| [updatedAt](updated-at.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [updatedAt](updated-at.md)(): Instant?<br>[js, jvm, native]<br>actual open fun [updatedAt](updated-at.md)(): Instant? |

## Inheritors

| Name |
|---|
| [VersionedRecord](../-versioned-record/index.md) |
