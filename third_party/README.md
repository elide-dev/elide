
# Third-party Code

This directory contains third-party projects which are built from source when building Elide. Projects are organized by
vendor or author, with each library underneath in a dedicated Git sub-module. Third-party projects come in a variety of
languages, so everything is built with a vanilla `Makefile`.

## Building

From the root project, you can do:
```
make third-party
```

Or, from inside `third_party`, you can just run `make`.

## Projects

- [Apple][0] [Pkl][1]: Safe, programmable, and scalable configuration language.
- [Astral][2] [Ruff][3]: Very fast linter for Python, written in Rust.
- [Astral][2] [UV][4]: Very fast Python/PyPI dependency resolver and installer.
- [esbuild][5]: Very fast JavaScript bundler and minifier.
- [GraalVM][7]: Pinned version for Elide's edge
- [grCUDA][6]: CUDA bindings for GraalVM
- [SQLite][11]: Pinned version for Elide's use

### GraalVM Language Engines

- [GraalJs][8]: GraalVM implementation of ECMA-compliant JS
- [GraalPython][9]: GraalVM implementation of `3.12.x`-compliant Python
- [TruffleRuby][10]: GraalVM implementation of Ruby, compatibile with MRI `3.2`

[0]: https://apple.com
[1]: https://github.com/apple/pkl
[2]: https://astral.sh
[3]: https://astral.sh/ruff
[4]: https://github.com/astral-sh/uv
[5]: https://esbuild.github.io
[6]: https://github.com/elide-dev/grcuda
[7]: https://github.com/oracle/graal
[8]: https://github.com/oracle/graaljs
[9]: https://github.com/oracle/graalpython
[10]: https://github.com/oracle/truffleruby
[11]: https://sqlite.org
