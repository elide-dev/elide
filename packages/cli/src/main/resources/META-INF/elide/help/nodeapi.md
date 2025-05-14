# Elide + Node.js API

Elide includes support for the Node.js API, although it is not feature-complete as of this writing; major functions and
constants which are popular are available, including the following modules:

- `assert`
- `buffer`
- `fs`
- `path`
- `stream`
- `zlib`

This support is evolving all the time, targeting Node.js 22 or greater.

## Usage

To use Node API modules, import them as normal:
```javascript
import { readFileSync } from "node:fs";
```

> [!TIP]
> It's good to use the `node` prefix to reduce ambiguity; this also enables this code to work on e.g. Deno. Elide will
> accept imports with or without the `node:` prefix.
