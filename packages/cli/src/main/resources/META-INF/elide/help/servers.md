# Servers

Elide has a built-in server which is powered by [Netty][0] and [Micronaut][1]. It's really fast. Features include:

- HTTP/2, HTTP/3, WebSockets
- TLS via OpenSSL or BoringSSL
- Non-blocking I/O by default

You can use this server from any language supported by Elide. The following samples are in JavaScript but don't have
to be.

```javascript
export default {
  async fetch(request: Request): Promise<Response> {
    return new Response("Hello!", { status: 200 })
  }
}
```

This is a simple server handler. To run it, just do:
```console
elide serve ./your/entrypoint.mts
```

Or, within a project, just:
```
elide serve
```

## Performance

On Linux with native transports, Elide can sustain about 800K RPS in JavaScript. Elide is independently benchmarked
through by [TechEmpower][2].

[0]: https://netty.io
[1]: https://micronaut.io
[2]: https://techempower.com/benchmarks/
