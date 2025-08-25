Summary
- Implements Node `dns` and `dns/promises` with basic A/AAAA/Reverse support and default result ordering.
- Fixes Node HTTP createServer handler and lifecycle semantics so `listen`/`close` are exposed and non-boolean returns don’t fall through.

What changed
- DNS:
  - `NodeDNS.kt`: `resolve`, `resolve4`, `resolve6`, `reverse`, `set/getDefaultResultOrder`, `getServers`; ENOTSUP stubs for other RR types.
  - `NodeDNSPromises.kt`: Promise variants of `resolve`, `resolve4`, `resolve6`, `reverse`; ENOTSUP rejections for unsupported RR types.
- HTTP:
  - `NodeHttp.kt`: ensure `(req, res)` dispatch and exposure of `listen`/`close`.
  - `GuestSimpleHandler.kt`: treat non-boolean returns as handled; forward `(req,res,ctx)` correctly.
  - `HttpServerConfig.kt`: enforce executable `onBind` callbacks via proxy method.

Behavioral notes
- DNS uses JVM resolution and filters IPv4/IPv6; `defaultResultOrder="ipv4first"` sorts v4 first.
- Reverse DNS returns hostname or empty list on failure. Promise APIs reject on errors/ENOTSUP.
- HTTP handler returns `true` by default if no explicit boolean, preventing accidental 404 fallthrough.

Tests
- `NodeHttpTest.kt` exercises server lifecycle and a basic GET flow.
- `NodeDnsPromisesTest.kt` covers Promise API `resolve`/`resolve4`/`resolve6`/`reverse`.
- Local runs:
  - HTTP: `.\gradlew.bat :packages:graalvm:test --tests "elide.runtime.node.NodeHttpTest" -i`
  - DNS: `.\gradlew.bat :packages:graalvm:test --tests "elide.runtime.node.NodeDnsPromisesTest" -i`

Follow-ups
- Add RR types (CNAME/MX/TXT/etc.), configurable resolvers, and broader HTTP streaming tests.
