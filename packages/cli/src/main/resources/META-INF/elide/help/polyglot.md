# Polyglot

Elide can run and load multiple languages, even within the same application. How does this work, and how can you use it?
This guide aims to answer those questions.

Help topics are available for each language; run `elide help <language>`.

Supported language engines in this binary:

- JavaScript & WASM (+ JSX)
- TypeScript (+ TSX)
- Python (v3.11)
- Java, Kotlin (JVM 24)

## Next-gen Interop

Elide is built on top of [GraalVM][0] and [Truffle][1]. Languages are implemented as Truffle interpreters, so each can
interoperate with the others out of the box. For example:

*`sample.py`*
```python
def hello():
    return "Hello from Python!"
```
*`sample.mts`*
```typescript
import sample from "./module.py"

console.log(sample.hello())
// prints: "Hello from Python!"
```

- No *serialization* takes place when calling between languages.
- *Inlining* and *JIT* can take place across languages.
- *Garbage collection* is cohesive across languages.

If you use more than one language within your application, you still have only one garbage collector and runtime
running.

[0]: https://graalvm.org
[1]: https://graalvm.org/latest/graalvm-as-a-platform
