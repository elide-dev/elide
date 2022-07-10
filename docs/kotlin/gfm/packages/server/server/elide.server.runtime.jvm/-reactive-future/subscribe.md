//[server](../../../index.md)/[elide.server.runtime.jvm](../index.md)/[ReactiveFuture](index.md)/[subscribe](subscribe.md)

# subscribe

[jvm]\
open override fun [subscribe](subscribe.md)(subscriber: Subscriber&lt;in [R](index.md)?&gt;)

Request Publisher to start streaming data.

This is a &quot;factory method&quot; and can be called multiple times, each time starting a new Subscription. Each Subscription will work for only a single Subscriber. A Subscriber should only subscribe once to a single Publisher. If the Publisher rejects the subscription attempt or otherwise fails it will signal the error via Subscriber.onError.

## Parameters

jvm

| | |
|---|---|
| subscriber | the Subscriber that will consume signals from this Publisher. |
