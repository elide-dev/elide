# pyright: reportMissingImports=false, reportUndefinedVariable=false
# Minimal Elide Flask app on GraalPy
# Requires Elide runtime with Python enabled and the Flask shim plugin active.

from elide_flask import Flask, request, Response  # type: ignore

app = Flask(__name__)

# Configure server: ensure Elide is reachable and auto-start the Netty server.
import sys
print("FLASK_SHIM_APP_START", file=sys.stderr, flush=True)

try:
  import polyglot  # type: ignore
  Elide = globals().get("Elide")
  if Elide is None:
    try:
      Elide = polyglot.import_value("Elide")
      print("FLASK_DEBUG: Imported Elide via polyglot.import_value", file=sys.stderr, flush=True)
    except Exception as e:
      print(f"FLASK_DEBUG: import_value failed: {e}", file=sys.stderr, flush=True)
      try:
        Elide = polyglot.eval("js", "Elide")
        print("FLASK_DEBUG: Resolved Elide via polyglot.eval('js','Elide')", file=sys.stderr, flush=True)
      except Exception as e2:
        print(f"FLASK_DEBUG: eval('js','Elide') failed: {e2}", file=sys.stderr, flush=True)
        Elide = None
except Exception as e:
  print(f"FLASK_DEBUG: polyglot import failed: {e}", file=sys.stderr, flush=True)
  Elide = None

if Elide is not None:
  try:
    import sys, os
    cfg = Elide.http.server.config
    cfg.autoStart = True
    cfg.host = "127.0.0.1"

    # Get port from system property (set by test), then PORT env, then 8080
    port = None
    try:
      # Check if elide.server.port system property is set (used by tests)
      # Use polyglot to access Java System class
      import polyglot  # type: ignore
      JavaSystem = polyglot.eval("java", "java.lang.System")
      port_prop = JavaSystem.getProperty("elide.server.port")
      if port_prop:
        port = int(port_prop)
        print(f"FLASK_DEBUG: Using port from system property: {port}", file=sys.stderr, flush=True)
    except Exception as e:
      print(f"FLASK_DEBUG: Failed to get system property: {e}", file=sys.stderr, flush=True)
    if port is None:
      try:
        port = int(os.environ.get("PORT", "8080"))
        print(f"FLASK_DEBUG: Using port from env/default: {port}", file=sys.stderr, flush=True)
      except Exception:
        port = 8080
    cfg.port = port
    try:
      cfg.onBind(lambda: print(f"BOUND {cfg.host}:{cfg.port}", file=sys.stderr, flush=True))
    except Exception:
      pass
    try:
      Elide.http.start()
    except Exception as e:
      print(f"FLASK_DEBUG: Elide.http.start() failed: {e}", file=sys.stderr, flush=True)

  except Exception as e:
    print(f"FLASK_DEBUG: Server config failed: {e}", file=sys.stderr, flush=True)

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
