from elide import Flask

# create and configure an app
app = Flask(__name__)

# basic app routing
@app.route("/")
def hello_world():
  return "<p>Hello, World!</p>"

# print(f"Starting server worker at http://localhost:3000")

# start the server, required for now
# app.bind()
