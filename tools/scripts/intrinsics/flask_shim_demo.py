# pyright: reportMissingImports=false
# Minimal Elide Flask shim demo on GraalPy
# Usage: evaluate this script in an Elide PolyglotContext with the HTTP server agent
# present; it will register routes and start the Netty server if configured for auto-start.

from elide_flask import Flask, request, Response  # type: ignore

app = Flask(__name__)

@app.route("/ping", methods=["GET"])  # returns plaintext
def ping():
  return "pong"

@app.route("/echo", methods=["POST"])  # echoes JSON
def echo():
  data = request.get_json() or {}
  return Response({"echo": data}, status=200)

