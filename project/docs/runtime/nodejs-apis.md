# Node APIs

Elide aims for near-complete Node.js API compatibility; where modules don't make sense or aren't possible to implement,
problems are noted below.

Most `npm` packages intended for `Node.js` environments will work with Elide out of the box; the best way to know for certain is to try it.

This page is updated regularly to reflect compatibility status of the latest version of Elide.
The information below reflects Elide's's compatibility with _Node.js v22_.

If you run into any bugs with a particular package, please [open an issue](https://github.com/elide-dev/elide/issues/new).
Opening issues for compatibility bugs helps us prioritize what to work on next.

## Legend

- 🔴 Not implemented, can't implement, won't implement; this is not a final state.
- 🟡 In progress, coming soon, under testing/experimental, available but not fully in compliance yet.
- 🟢 Fully implemented; total or near-total compliance.

## Built-in modules

### [`node:assert`](https://nodejs.org/api/assert.html)

🟡 Coming soon.

### [`node:async_hooks`](https://nodejs.org/api/async_hooks.html)

🔴 Not implemented.

### [`node:buffer`](https://nodejs.org/api/buffer.html)

🟡 Coming soon.

### [`node:child_process`](https://nodejs.org/api/child_process.html)

🟡 Coming soon.

### [`node:cluster`](https://nodejs.org/api/cluster.html)

🔴 Not implemented.

### [`node:console`](https://nodejs.org/api/console.html)

🟡 Coming soon.

### [`node:crypto`](https://nodejs.org/api/crypto.html)

🔴 Not implemented.

### [`node:dgram`](https://nodejs.org/api/dgram.html)

🔴 Not implemented.

### [`node:diagnostics_channel`](https://nodejs.org/api/diagnostics_channel.html)

🔴 Not implemented.

### [`node:dns`](https://nodejs.org/api/dns.html)

🟢 Fully implemented via native Rust bindings using [hickory-resolver](https://github.com/hickory-dns/hickory-dns).

**Callback API (`dns`):**
- `lookup(hostname, [options], callback)` - Uses `getaddrinfo` (respects `/etc/hosts`, NSS)
- `lookupService(address, port, callback)` - Uses `getservbyport` for service names
- `resolve(hostname, [rrtype], callback)` - DNS resolution (A, AAAA, MX, TXT, etc.)
- `resolve4`, `resolve6`, `resolveCname`, `resolveNs`, `resolvePtr`, `resolveMx`, `resolveSrv`, `resolveTxt`, `resolveNaptr`, `resolveCaa`, `resolveTlsa`, `resolveSoa`, `resolveAny`
- `reverse(ip, callback)` - Reverse DNS lookup
- `getServers()`, `setServers(servers)` - DNS server configuration
- `getDefaultResultOrder()`, `setDefaultResultOrder(order)` - IPv4/IPv6 ordering

**Promise API (`dns.promises`):** All callback methods available as promise-returning functions.

**Resolver class:** `dns.Resolver` and `dns.promises.Resolver` for isolated resolver instances.

**Notes:**
- Native implementation uses Rust JNI bindings for performance
- `lookup()` uses libc `getaddrinfo` (thread-safe via mutex) for OS-level resolution
- `resolve*()` methods use hickory-resolver for pure DNS queries
- System DNS servers detected automatically via `/etc/resolv.conf`

### [`node:domain`](https://nodejs.org/api/domain.html)

🔴 Not implemented.

### [`node:events`](https://nodejs.org/api/events.html)

🟡 Coming soon.

### [`node:fs`](https://nodejs.org/api/fs.html)

🟡 Some basic methods are implemented (`readFile`, `readFileSync`, `writeFile`, `writeFileSync`, etc.).

### [`node:http`](https://nodejs.org/api/http.html)

🔴 Not implemented.

### [`node:http2`](https://nodejs.org/api/http2.html)

🔴 Not implemented.

### [`node:https`](https://nodejs.org/api/https.html)

🔴 Not implemented.

### [`node:inspector`](https://nodejs.org/api/inspector.html)

🔴 Not implemented.

### [`node:module`](https://nodejs.org/api/module.html)

🔴 Not implemented.

### [`node:net`](https://nodejs.org/api/net.html)

🔴 Not implemented.

### [`node:os`](https://nodejs.org/api/os.html)

🟡 Coming soon.

### [`node:path`](https://nodejs.org/api/path.html)

🟡 Coming soon.

### [`node:perf_hooks`](https://nodejs.org/api/perf_hooks.html)

🔴 Not implemented.

### [`node:process`](https://nodejs.org/api/process.html)

🟡 See [`process`](#process) Global.

### [`node:punycode`](https://nodejs.org/api/punycode.html)

🔴 Not implemented.

### [`node:querystring`](https://nodejs.org/api/querystring.html)

🔴 Not implemented.

### [`node:readline`](https://nodejs.org/api/readline.html)

🔴 Not implemented.

### [`node:repl`](https://nodejs.org/api/repl.html)

🔴 Not implemented.

### [`node:stream`](https://nodejs.org/api/stream.html)

🟡 Coming soon.

### [`node:string_decoder`](https://nodejs.org/api/string_decoder.html)

🔴 Not implemented.

### [`node:sys`](https://nodejs.org/api/util.html)

🟡 See [`node:util`](#node-util).

### [`node:test`](https://nodejs.org/api/test.html)

🔴 Not implemented.

### [`node:timers`](https://nodejs.org/api/timers.html)

🔴 Not implemented.

### [`node:tls`](https://nodejs.org/api/tls.html)

🔴 Not implemented.

### [`node:trace_events`](https://nodejs.org/api/tracing.html)

🔴 Not implemented.

### [`node:tty`](https://nodejs.org/api/tty.html)

🔴 Not implemented.

### [`node:url`](https://nodejs.org/api/url.html)

🟡 Coming soon.

### [`node:util`](https://nodejs.org/api/util.html)

🟡 Mostly polyfilled.

### [`node:v8`](https://nodejs.org/api/v8.html)

🔴 Not implemented.

### [`node:vm`](https://nodejs.org/api/vm.html)

🔴 Not implemented.

### [`node:wasi`](https://nodejs.org/api/wasi.html)

🔴 Not implemented.

### [`node:worker_threads`](https://nodejs.org/api/worker_threads.html)

🔴 Not implemented.

### [`node:zlib`](https://nodejs.org/api/zlib.html)

🔴 Not implemented.

## Globals

The table below lists all globals implemented by Node.js and Bun's current compatibility status.

### [`AbortController`](https://developer.mozilla.org/en-US/docs/Web/API/AbortController)

🔴 Not implemented.

### [`AbortSignal`](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal)

🔴 Not implemented.

### [`Blob`](https://developer.mozilla.org/en-US/docs/Web/API/Blob)

🔴 Not implemented.

### [`Buffer`](https://nodejs.org/api/buffer.html#class-buffer)

🔴 Not implemented.

### [`ByteLengthQueuingStrategy`](https://developer.mozilla.org/en-US/docs/Web/API/ByteLengthQueuingStrategy)

🔴 Not implemented.

### [`__dirname`](https://nodejs.org/api/globals.html#__dirname)

🔴 Not implemented.

### [`__filename`](https://nodejs.org/api/globals.html#__filename)

🔴 Not implemented.

### [`atob()`](https://developer.mozilla.org/en-US/docs/Web/API/atob)

🟢 Fully implemented.

### [`BroadcastChannel`](https://developer.mozilla.org/en-US/docs/Web/API/BroadcastChannel)

🔴 Not implemented.

### [`btoa()`](https://developer.mozilla.org/en-US/docs/Web/API/btoa)

🟢 Fully implemented.

### [`clearImmediate()`](https://developer.mozilla.org/en-US/docs/Web/API/Window/clearImmediate)

🟡 Coming soon.

### [`clearInterval()`](https://developer.mozilla.org/en-US/docs/Web/API/Window/clearInterval)

🟡 Coming soon.

### [`clearTimeout()`](https://developer.mozilla.org/en-US/docs/Web/API/Window/clearTimeout)

🟡 Coming soon.

### [`CompressionStream`](https://developer.mozilla.org/en-US/docs/Web/API/CompressionStream)

🔴 Not implemented.

### [`console`](https://developer.mozilla.org/en-US/docs/Web/API/console)

🟢 Fully implemented.

### [`CountQueuingStrategy`](https://developer.mozilla.org/en-US/docs/Web/API/CountQueuingStrategy)

🔴 Not implemented.

### [`Crypto`](https://developer.mozilla.org/en-US/docs/Web/API/Crypto)

🔴 Not implemented.

### [`SubtleCrypto (crypto)`](https://developer.mozilla.org/en-US/docs/Web/API/crypto)

🔴 Not implemented.

### [`CryptoKey`](https://developer.mozilla.org/en-US/docs/Web/API/CryptoKey)

🔴 Not implemented.

### [`CustomEvent`](https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent)

🟡 Coming soon.

### [`DecompressionStream`](https://developer.mozilla.org/en-US/docs/Web/API/DecompressionStream)

🔴 Not implemented.

### [`Event`](https://developer.mozilla.org/en-US/docs/Web/API/Event)

🟡 Coming soon.

### [`EventTarget`](https://developer.mozilla.org/en-US/docs/Web/API/EventTarget)

🟡 Coming soon.

### [`exports`](https://nodejs.org/api/globals.html#exports)

🟢 Fully implemented.

### [`fetch`](https://developer.mozilla.org/en-US/docs/Web/API/fetch)

🟡 Coming soon.

### [`FormData`](https://developer.mozilla.org/en-US/docs/Web/API/FormData)

🔴 Not implemented.

### [`global`](https://nodejs.org/api/globals.html#global)

🟢 Implemented. This is an object containing all objects in the global namespace. It's rarely referenced directly, as its contents are available without an additional prefix, e.g. `__dirname` instead of `global.__dirname`.

### [`globalThis`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/globalThis)

🟢 Aliases to `global`.

### [`Headers`](https://developer.mozilla.org/en-US/docs/Web/API/Headers)

🔴 Not implemented.

### [`MessageChannel`](https://developer.mozilla.org/en-US/docs/Web/API/MessageChannel)

🔴 Not implemented.

### [`MessageEvent`](https://developer.mozilla.org/en-US/docs/Web/API/MessageEvent)

🔴 Not implemented.

### [`MessagePort`](https://developer.mozilla.org/en-US/docs/Web/API/MessagePort)

🔴 Not implemented.

### [`module`](https://nodejs.org/api/globals.html#module)

🟢 Fully implemented.

### [`PerformanceEntry`](https://developer.mozilla.org/en-US/docs/Web/API/PerformanceEntry)

🔴 Not implemented.

### [`PerformanceMark`](https://developer.mozilla.org/en-US/docs/Web/API/PerformanceMark)

🔴 Not implemented.

### [`PerformanceMeasure`](https://developer.mozilla.org/en-US/docs/Web/API/PerformanceMeasure)

🔴 Not implemented.

### [`PerformanceObserver`](https://developer.mozilla.org/en-US/docs/Web/API/PerformanceObserver)

🔴 Not implemented.

### [`PerformanceObserverEntryList`](https://developer.mozilla.org/en-US/docs/Web/API/PerformanceObserverEntryList)

🔴 Not implemented.

### [`PerformanceResourceTiming`](https://developer.mozilla.org/en-US/docs/Web/API/PerformanceResourceTiming)

🔴 Not implemented.

### [`performance`](https://developer.mozilla.org/en-US/docs/Web/API/performance)

🔴 Not implemented.

### [`process`](https://nodejs.org/api/process.html)

🟡 Mostly implemented.

### [`queueMicrotask()`](https://developer.mozilla.org/en-US/docs/Web/API/queueMicrotask)

🔴 Not implemented.

### [`ReadableByteStreamController`](https://developer.mozilla.org/en-US/docs/Web/API/ReadableByteStreamController)

🔴 Not implemented.

### [`ReadableStream`](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStream)

🔴 Not implemented.

### [`ReadableStreamBYOBReader`](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStreamBYOBReader)

🔴 Not implemented.

### [`ReadableStreamBYOBRequest`](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStreamBYOBRequest)

🔴 Not implemented.

### [`ReadableStreamDefaultController`](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStreamDefaultController)

🔴 Not implemented.

### [`ReadableStreamDefaultReader`](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStreamDefaultReader)

🔴 Not implemented.

### [`require()`](https://nodejs.org/api/globals.html#require)

🟢 Fully implemented, including [`require.main`](https://nodejs.org/api/modules.html#requiremain), [`require.cache`](https://nodejs.org/api/modules.html#requirecache), [`require.resolve`](https://nodejs.org/api/modules.html#requireresolverequest-options)

### [`Response`](https://developer.mozilla.org/en-US/docs/Web/API/Response)

🔴 Not implemented.

### [`Request`](https://developer.mozilla.org/en-US/docs/Web/API/Request)

🔴 Not implemented.

### [`setImmediate()`](https://developer.mozilla.org/en-US/docs/Web/API/Window/setImmediate)

🟡 Coming soon.

### [`setInterval()`](https://developer.mozilla.org/en-US/docs/Web/API/Window/setInterval)

🟡 Coming soon.

### [`setTimeout()`](https://developer.mozilla.org/en-US/docs/Web/API/Window/setTimeout)

🟡 Coming soon.

### [`structuredClone()`](https://developer.mozilla.org/en-US/docs/Web/API/structuredClone)

🟢 Fully implemented.

### [`SubtleCrypto`](https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto)

🔴 Not implemented.

### [`DOMException`](https://developer.mozilla.org/en-US/docs/Web/API/DOMException)

🔴 Not implemented.

### [`TextDecoder`](https://developer.mozilla.org/en-US/docs/Web/API/TextDecoder)

🟡 Polyfilled.

### [`TextDecoderStream`](https://developer.mozilla.org/en-US/docs/Web/API/TextDecoderStream)

🔴 Not implemented.

### [`TextEncoder`](https://developer.mozilla.org/en-US/docs/Web/API/TextEncoder)

🟡 Polyfilled.

### [`TextEncoderStream`](https://developer.mozilla.org/en-US/docs/Web/API/TextEncoderStream)

🔴 Not implemented.

### [`TransformStream`](https://developer.mozilla.org/en-US/docs/Web/API/TransformStream)

🔴 Not implemented.

### [`TransformStreamDefaultController`](https://developer.mozilla.org/en-US/docs/Web/API/TransformStreamDefaultController)

🔴 Not implemented.

### [`URL`](https://developer.mozilla.org/en-US/docs/Web/API/URL)

🟢 Fully implemented; approaches full compliance.

### [`URLSearchParams`](https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams)

🟡 Coming soon.

### [`WebAssembly`](https://nodejs.org/api/globals.html#webassembly)

🟢 Fully implemented.

### [`WritableStream`](https://developer.mozilla.org/en-US/docs/Web/API/WritableStream)

🔴 Not implemented.

### [`WritableStreamDefaultController`](https://developer.mozilla.org/en-US/docs/Web/API/WritableStreamDefaultController)

🔴 Not implemented.

### [`WritableStreamDefaultWriter`](https://developer.mozilla.org/en-US/docs/Web/API/WritableStreamDefaultWriter)

🔴 Not implemented.
