// retrieve the binding from global symbols
console.log("Resolving server intrinsics");
const server = Elide.http;
const router = server.router;
const config = server.config;

// define local dependencies to demonstrate guest references
// (note that this counter will be thread-local)
const decorations = ["🚀", "👋", "🙂", "👍", "🎉", "✨"];
let counter = 0;

// iterate over different emojis for an easy visual demo
function getMessage() {
  const message = `${decorations[counter % decorations.length]} Hello Elide!`;
  counter += 1;

  return message;
}

// define route handlers
console.log("Configuring route handlers");
router.handle("GET", "/hello", (request, response, context) => {
  response.send(200, getMessage());
});

// specific options for the Netty backend
console.log("Configuring server transport");
config.transport = "nio";

// set the port to listen on and register a callback
console.log("Configuring binding options");
config.port = 3000;
config.onBind(() => {
  console.log("Server listening! 🚀");
});

// start listening for connections
// (this call is inert when issued from handler threads)
console.log("✨ Configuration finished ✨");
server.start();