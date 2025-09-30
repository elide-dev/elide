# create and configure an app
app = Flask(__name__)

# basic app routing
@app.get("/hello")
@app.get("/hello/<name>")
def hello_world():
  return f"<p>Hello, World! (method={request.method}, path={request.path})</p>"

@app.post("/hello/")
def hello_world():
  return "<p>(POST) Hello, World!</p>"

@app.post("/bye")
def hello_world():
  return "<p>Goodbye, World!</p>"

# start the server, required for now
app.bind()

print(f"Server listening at http://localhost:3000")
