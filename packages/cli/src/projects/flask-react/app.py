from elide import Flask, react, request

# create and configure an app
app = Flask(__name__)
home = react("index.tsx")

# basic app routing
@app.route("/")
def hello_world():
    user_agent = request.headers.get("User-Agent", "<not set>")
    return home(user_agent = user_agent)

