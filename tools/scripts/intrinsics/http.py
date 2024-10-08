# retrieve the binding from global symbols
print("Resolving server intrinsics")
server = Elide.http
router = server.getRouter()
config = server.getConfig()


# define local dependencies to demonstrate guest references
# (note that this counter will be thread-local)
class Decorations:
  decorations = ["🚀", "👋", "🙂", "👍", "🎉", "✨"]
  counter = 0

  def get_decoration(self):
    decor = self.decorations[self.counter % len(self.decorations)]
    self.counter += 1

    return decor


decorations = Decorations()


# iterate over different emojis for an easy visual demo
def get_message():
  return f"{decorations.get_decoration()} Hello Elide.py!"


# define route handlers
print("Configuring route handlers")


def hello(request, response, context):
  response.send(200, get_message())


router.handle("GET", "/hello", hello)

# specific options for the Netty backend
print("Configuring server transport")
config.setTransport("nio")

# set the port to listen on and register a callback
print("Configuring binding options")


def bind_listener():
  print("Server listening! 🚀")


config.setPort(3000)
config.onBind(bind_listener)

# start listening for connections
# (this call is inert when issued from handler threads)
print("✨ Configuration finished ✨")
server.start()
