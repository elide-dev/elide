// retrieve the binding from global symbols
console.log("🏗️ Resolving server intrinsics");
const server = Elide.http;
const router = server.router;
const config = server.config;

// define local dependencies to demonstrate guest references
// (note that this counter will be thread-local)
const decorations = ["🚀", "👋", "🙂", "👍", "🎉", "✨"];
let counter = 0;

// iterate over different emojis for an easy visual demo
function getMessage() {
  const message = `${decorations[counter % decorations.length]}Hello %s!`;
  counter += 1;

  return message;
}

// add middleware
console.log("🚧Configuring middleware")
router.handle(null, null, (request, response, context) => {
  // inject the response message into the context to be accessed by other handlers
  context.message = getMessage()

  // pass the request to the next handler
  return true
})

router.handle(null, null, (request, response, context) => {
  // print the message as the request passes through this handler
  console.log(`📢Responding with message template: "${context.message}"`)

  // pass the request to the next handler
  return true
})

// define route handlers
console.log("🚧Configuring route handlers");
router.handle("GET", "/hello", (request, response, context) => {
  response.send(200, context.message.replace("%s", "Elide"));
});

router.handle("GET", "/hello/:name", (request, response, context) => {
  response.send(200, context.message.replace("%s", context.params.name));
});

// specific options for the Netty backend
console.log("🚧Configuring server transport");
config.transport = "nio";

// set the port to listen on and register a callback
console.log("🚧Configuring binding options");
config.port = 3000;
config.onBind(() => {
  console.log("Server listening! 🚀");
});

// start listening for connections
// (this call is inert when issued from handler threads)
console.log("✨Configuration finished✨");
server.start();