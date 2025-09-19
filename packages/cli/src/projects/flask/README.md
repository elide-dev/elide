# Elide Flask (GraalPy) sample

This sample demonstrates a minimal Flask-compatible API running on Elide (GraalPy) and Netty.

Endpoints:
- GET /ping -> "pong"
- POST /echo (JSON body) -> { "echo": { ... } }

Run:
- elide serve src/app.py --port 8080
- or: elide run src/app.py

Notes:
- The Flask-compatible shim is provided by Elide's Python plugin and Netty HTTP intrinsics. No CPython or Flask package is required.

