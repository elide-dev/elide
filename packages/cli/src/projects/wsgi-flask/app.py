from flask import Flask
from elide import wsgi as elide_wsgi

app = Flask(__name__)

@app.route("/")
def hello_world():
    return "<p>Hello, World!</p>"

print("Trying a test thing")

# with app.test_request_context("/", method="GET"):
#     print("Using test context")
#     print(hello_world())

elide_wsgi(app)
