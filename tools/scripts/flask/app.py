# from flask import Flask

# ---------------------------------------------------------------
request = __elide_flask.request
abort = __elide_flask.abort

_flask_class = __elide_flask

class Flask:
    def __init__(self, name: str):
        self.bridge = _flask_class(name)

    def route(self, rule: str, **options):
        def handler_wrapper(handler):
            methods = options["methods"] if options["methods"] is not None else ["GET"]

            if hasattr(handler, "_elide_flask_handler"):
                # already wrapped, just register it
                self.bridge.route(handler.__name__, rule, methods, handler)
                return handler

            # not wrapped yet
            def dispatcher(args):
                return handler(**args)

            dispatcher._elide_flask_handler = True
            self.bridge.route(handler.__name__, rule, methods, dispatcher)

            return dispatcher
        return handler_wrapper

    def head(self, rule: str):
        return self.route(rule, methods=["HEAD"])

    def options(self, rule: str):
        return self.route(rule, methods=["OPTIONS"])

    def get(self, rule: str):
        return self.route(rule, methods=["GET"])

    def post(self, rule: str):
        return self.route(rule, methods=["POST"])

    def put(self, rule: str):
        return self.route(rule, methods=["PUT"])

    def patch(self, rule: str):
        return self.route(rule, methods=["PATCH"])

    def delete(self, rule: str):
        return self.route(rule, methods=["DELETE"])

    def bind(self):
        self.bridge.bind()

# ---------------------------------------------------------------


# create and configure an app
app = Flask(__name__)

# basic app routing
@app.get("/hello")
@app.get("/hello/<name>")
def hello_world(name=None):
    if request.args.get("yeet") == "true":
        abort(418)

    message = f"Hello {name if name is not None else 'World'}!"

    if request.args.get("flair") == "true":
        message += "🎉"

    return f"<p>{message}</p>"

@app.post("/hello/")
def hello_world_post():
  return "<p>(POST) Hello, World!</p>"

@app.get("/bye")
def bye_world():
  return "<p>Goodbye, World!</p>"

# start the server, required for now
app.bind()

print("Server listening at http://localhost:3000")
