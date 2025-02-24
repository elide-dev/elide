
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
- [SQLite][11]: Pinned version for Elide's use
- [Google][12] [BoringSSL][13]: Google's fixed-up fork of OpenSSL.
- [Apache][14] [APR (Apache Portable Runtime)][15]: Cross-platform runtime utilities
- [Cloudflare][16]'s fork of [zlib][17]: Fast compression library

[0]: https://apple.com
[1]: https://github.com/apple/pkl
[4]: https://github.com/astral-sh/uv
[11]: https://sqlite.org
[12]: https://google.com
[13]: https://boringssl.googlesource.com/boringssl
[14]: https://apache.org
[15]: https://apr.apache.org/
[16]: https://cloudflare.com
[17]: https://github.com/cloudflare/zlib
[18]: https://oracle.com
[19]: https://graalvm.org

