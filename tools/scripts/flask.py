# create and configure an app
app = Flask(__name__)

# basic app routing
@app.route("/")
def hello_world(context):
  context.status(200)
  return "<p>Hello, World!</p>"

# start the server, required for now
port = 3000
app.bind()

print(f"Server listening at http://localhost:{port}")
