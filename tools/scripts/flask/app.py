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

@app.get("/redirect")
def point():
    return redirect(url_for('hello_world', name='Dario', flair='true'))
