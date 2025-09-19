# pyright: reportMissingImports=false, reportUndefinedVariable=false
# Minimal Elide Flask app on GraalPy
# Requires Elide runtime with Python enabled and the Flask shim plugin active.

from elide_flask import Flask, request, Response  # type: ignore

app = Flask(__name__)

# Configure server: ensure Elide is reachable and auto-start the Netty server.
try:
  import polyglot  # type: ignore
  Elide = globals().get("Elide") or polyglot.eval("js", "Elide")
except Exception:
  Elide = None

if Elide is not None:
  try:
    import sys, os
    cfg = Elide.http.config
    cfg.autoStart = True
    # Prefer --port from CLI argv, then PORT env, then 8080
    port = None
    try:
      args = list(sys.argv)
      if "--port" in args:
        i = args.index("--port")
        if i + 1 < len(args):
          port = int(args[i + 1])
    except Exception:
      port = None
    if port is None:
      port = int(os.environ.get("PORT", "8080"))
    cfg.port = port
  except Exception:
    pass

@app.route("/ping", methods=["GET"])  # returns plaintext
def ping():
  return "pong"

@app.route("/echo", methods=["POST"])  # echoes JSON
def echo():
  data = request.get_json() or {}
  return Response({"echo": data}, status=200)

# Fallback: ensure endpoints are bound via the Elide router even if the shim did not attach
if Elide is not None:
  try:
    # /ping
    def _ping(req, resp, ctx):
      resp.send(200, "pong")
      return True
    Elide.http.router.handle("GET", "/ping", _ping)

    # /echo
    def _echo(req, resp, ctx):
      import json
      try:
        # Read raw body from request proxy if available; fall back to empty
        d = request.get_json() or {}
      except Exception:
        d = {}
      resp.header("Content-Type", "application/json")
      resp.send(200, json.dumps({"echo": d}))
      return True
    Elide.http.router.handle("POST", "/echo", _echo)
  except Exception:
    pass
