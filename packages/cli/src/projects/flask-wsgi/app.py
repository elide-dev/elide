from flask import Flask, request

def make_app(server_name: str, server_version: str):
  print("Starting app worker")
  app = Flask(__name__)

  base_headers = { "Server": f"{server_name} {server_version}" }

  @app.route("/")
  def hello_world():
    return ("<p>Hello, World!</p>", base_headers)

  @app.post("/echo")
  def echo():
    print(f"Request content length: {request.content_length}")

    data = request.data
    print(f"Request received: {data}")

    return (data, base_headers)

  return app
