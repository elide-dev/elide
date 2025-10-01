from elide import Flask, request, make_response, redirect, url_for

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

    response = make_response(f"<p>{message}</p>", 202)
    response.headers["X-Elide-Demo"] = "Flask"
    return response

@app.get("/pointer")
def point():
    return redirect(url_for('hello_world', name='Dario', flair='true'))

@app.post("/hello/")
def hello_world_post():
  return "<p>(POST) Hello, World!</p>"

@app.get("/bye")
def bye_world():
  return "<p>Goodbye, World!</p>"

@app.get("/content")
def response_content():
    # gen = (f"Chunk {x}\n" for x in range(5))
    # return (gen, 201, {"Server": "Elide"})
    json = {
        "Hello": "World",
        "messages": [
            "This is a message",
            "This is another message",
            ["This", "one", "is", "a", "list"],
            {
                "This": "other",
                "one": "is",
                "an": "object"
            }
        ]
    }

    return (json, 201, {"Server": "Elide"})

# start the server, required for now
app.bind()

print("Server listening at http://localhost:3000")
