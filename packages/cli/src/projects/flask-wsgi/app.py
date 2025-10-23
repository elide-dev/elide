from flask import Flask

def make_app(server_name: str, server_version: str):
  print("Starting app worker")
  app = Flask(__name__)

  @app.route("/")
  def hello_world():
    headers = { "Server": f"{server_name} {server_version}" }
    return ("<p>Hello, World!</p>", headers)

  return app
