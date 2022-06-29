//[model](../../../index.md)/[elide.model](../index.md)/[AppRecord](index.md)

# AppRecord

[common]\
expect interface [AppRecord](index.md)&lt;[K](index.md), [M](index.md) : [WireMessage](../-wire-message/index.md)&gt;

Describes the expected interface for model objects which are designated as records.

Records, within the scope of an Elide application, are [AppModel](../-app-model/index.md) objects which comply with an extended set of API guarantees:

- 
   Like [AppModel](../-app-model/index.md) instances, [AppRecord](index.md)s must be paired to a [WireMessage](index.md) (usually a protocol buffer message).
- 
   Records must export some annotated field as their designated [id](id.md), which should resolve to a stable type [K](index.md).
- 
   Where applicable, records must export their [parentId](parent-id.md), which should resolve to a matching key type [K](index.md).
- 
   Where applicable, records must export an annotated [displayName](display-name.md) field for use in form UIs and so forth.

Additional extensions of the [AppRecord](index.md) type form supersets of these guarantees:

- 
   [StampedRecord](../-stamped-record/index.md) instances carry [StampedRecord.createdAt](../-stamped-record/created-at.md) and [StampedRecord.updatedAt](../-stamped-record/updated-at.md) timestamps.
- 
   [VersionedRecord](../-versioned-record/index.md) instances carry a [VersionedRecord.version](../-versioned-record/version.md) property for optimistic concurrency control.

Generally speaking, [AppModel](../-app-model/index.md) instances correspond with objects which are serialized and exchanged by an application but are not always addressable or persistent. [AppRecord](index.md) objects are expected to be identified (perhaps with type annotations), and typically correspond to database records which need CRUD-like operations.

[js, jvm, native]\
actual interface [AppRecord](index.md)&lt;[K](index.md), [M](index.md) : [WireMessage](../-wire-message/index.md)&gt;

Describes the expected interface for model objects which are designated as records.

Records, within the scope of an Elide application, are [AppModel](../-app-model/index.md) objects which comply with an extended set of API guarantees:

- 
   Like [AppModel](../-app-model/index.md) instances, [AppRecord](index.md)s must be paired to a [WireMessage](index.md) (usually a protocol buffer message).
- 
   Records must export some annotated field as their designated [id](id.md), which should resolve to a stable type [K](index.md).
- 
   Where applicable, records must export their [parentId](parent-id.md), which should resolve to a matching key type [K](index.md).
- 
   Where applicable, records must export an annotated [displayName](display-name.md) field for use in form UIs and so forth.

Additional extensions of the [AppRecord](index.md) type form supersets of these guarantees:

- 
   [StampedRecord](../-stamped-record/index.md) instances carry [StampedRecord.createdAt](../-stamped-record/created-at.md) and [StampedRecord.updatedAt](../-stamped-record/updated-at.md) timestamps.
- 
   [VersionedRecord](../-versioned-record/index.md) instances carry a [VersionedRecord.version](../-versioned-record/version.md) property for optimistic concurrency control.

Generally speaking, [AppModel](../-app-model/index.md) instances correspond with objects which are serialized and exchanged by an application but are not always addressable or persistent. [AppRecord](index.md) objects are expected to be identified (perhaps with type annotations), and typically correspond to database records which need CRUD-like operations.

## Functions

| Name | Summary |
|---|---|
| [displayName](display-name.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [displayName](display-name.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>[js, jvm, native]<br>actual open fun [displayName](display-name.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [id](id.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [id](id.md)(): [K](index.md)?<br>[js, jvm, native]<br>actual open fun [id](id.md)(): [K](index.md)? |
| [parentId](parent-id.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [parentId](parent-id.md)(): [K](index.md)?<br>[js, jvm, native]<br>actual open fun [parentId](parent-id.md)(): [K](index.md)? |

## Inheritors

| Name |
|---|
| [StampedRecord](../-stamped-record/index.md) |
