from flask import Flask, request
from elide import react


def make_app(server_name: str, server_version: str):
  print("Starting app worker")
  app = Flask(__name__)

  home = react("index.tsx")
  base_headers = { "Server": f"{server_name} {server_version}" }

  @app.route("/")
  def root():
    user_agent = request.headers.get("User-Agent", "<not set>")
    return (home(user_agent = user_agent), base_headers)

  @app.get("/hello")
  def hello():
    return ("<p>Hello, World!</p>", base_headers)

  @app.post("/echo")
  def echo():
    print(f"Request content length: {request.content_length}")

    data = request.data
    print(f"Request received: {data}")

    return (data, base_headers)

  return app
