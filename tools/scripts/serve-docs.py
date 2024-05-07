import http.server
import socketserver

PORT = 8000
DIRECTORY = "docs"

class Handler(http.server.SimpleHTTPRequestHandler):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, directory=DIRECTORY, **kwargs)

with socketserver.TCPServer(("", PORT), Handler) as httpd:
  print("serving at port", PORT)
  httpd.serve_forever()
