# Elide Node API Compatibility Matrix

Legend: Implemented / Partial / Missing

- Link each module to PRs and tests; update on every PR

| Module | Status | Gaps / Notes | Tests | Linked PR(s) |
|---|---|---|---|---|
| assert (+strict) | Partial | Surface present via runtime shims; deeper invariants TBD | ✅ basic |  |
| buffer | Implemented |  | ✅ |  |
| child_process | Missing | Stub only | ❌ |  |
| cluster | Partial | Mostly stubs | ❌ |  |
| console | Implemented |  | ✅ |  |
| crypto | Partial | Subsets mapped to WebCrypto; Node-specific APIs TBD | ✅ subset |  |
| dgram | Missing | Module scaffold present | ❌ |  |
| diagnostics_channel | Missing |  | ❌ |  |
| dns | Partial | A/AAAA/reverse; ENOTSUP others; defaultResultOrder | ✅ | #1617 |
| dns/promises | Partial | Promise variants for the above | ✅ | #1617 |
| domain | Partial |  | ⚠️ |  |
| events | Partial | EventEmitter/EventTarget implemented; module facade wired | ✅ |  |
| fs | Partial | readFile/writeFile sync/async; more ops TBD | ✅ |  |
| fs/promises | Partial | readFile/writeFile, mkdir, access | ✅ |  |
| http | Partial | createServer + minimal ServerResponse; streaming/backpressure TBD | ✅ | #1617, #1619 |
| http2 | Partial | Stubs; behavior TBD | ⚠️ |  |
| https | Partial | Wrapper TBD | ⚠️ | #1619 (follow-up planned) |
| inspector | Partial |  | ⚠️ |  |
| inspector/promises | Partial |  | ⚠️ |  |
| module | Partial | builtinModules/isBuiltin/createRequire | ✅ | #1619 |
| net | Partial | Client/server basics TBD | ⚠️ |  |
| os | Partial |  | ✅ |  |
| path | Partial | posix/win32 variants; edge cases/UNC TBD | ✅ |  |
| perf_hooks | Partial |  | ⚠️ |  |
| process | Implemented |  | ✅ |  |
| punycode | Missing |  | ❌ |  |
| querystring | Partial | Legacy minimal | ⚠️ |  |
| readline | Partial |  | ⚠️ |  |
| readline/promises | Partial |  | ⚠️ |  |
| repl | Missing |  | ❌ |  |
| stream | Partial | Core types present | ✅ |  |
| stream/consumers | Partial | Some consumers present | ✅ |  |
| stream/promises | Partial | finished/pipeline implemented; more tests in #1618 | ✅ | #1618 |
| stream/web | Partial |  | ✅ |  |
| string_decoder | Implemented |  | ✅ |  |
| test | N/A | Out of scope |  |  |
| timers | Implemented | Node-facing module wired to JsTimers | ✅ | this PR |
| timers/promises | Implemented | setTimeout/setImmediate (promises) | ✅ | this PR |
| tls | Partial | Stub | ❌ |  |
| trace_events | Missing |  | ❌ |  |
| tty | Partial | Stub | ❌ |  |
| url | Partial | Helpers implemented; more parity possible | ✅ | #1619, this PR |
| util | Partial | promisify/callbackify/inspect/types subset | ✅ |  |
| v8 | Missing |  | ❌ |  |
| vm | Partial |  | ⚠️ |  |
| wasi | Missing |  | ❌ |  |
| worker_threads | Partial |  | ⚠️ |  |
| zlib | Partial |  | ⚠️ |  |

Notes:
- Do not duplicate work in #1617/#1618/#1619; build on top
- When expanding a module, update this file and add docs in docs/node/<module>.md

