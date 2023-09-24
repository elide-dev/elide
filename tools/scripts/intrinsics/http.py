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

  def getDecoration(self):
    decor = self.decorations[self.counter % len(self.decorations)]
    self.counter += 1

    return decor


decorations = Decorations()


# iterate over different emojis for an easy visual demo
def getMessage():
  return f"{decorations.getDecoration()} Hello Elide.py!\n"


# define route handlers
print("Configuring route handlers")


def hello(request, response, context):
  response.send(200, getMessage())


router.handle("GET", "/hello", hello)

# specific options for the Netty backend
print("Configuring server transport")
config.setTransport("nio")

# set the port to listen on and register a callback
print("Configuring binding options")


def bindListener():
  print("Server listening! 🚀")


config.setPort(3000)
config.onBind(bindListener)

# start listening for connections
# (this call is inert when issued from handler threads)
print("✨ Configuration finished ✨")
server.start()
