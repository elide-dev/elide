// retrieve the binding from global symbols
console.log("Resolving server intrinsics");
const router = Elide.http.router;
const config = Elide.http.config;

// define local dependencies to demonstrate guest references
// (note that this counter will be thread-local)
const decorations = ["🚀", "👋", "🙂", "👍", "🎉", "✨"];
let counter = 0;

// iterate over different emojis for an easy visual demo
function getMessage() {
  const message = `${decorations[counter % decorations.length]} Hello Elide!\n`;
  counter += 1;

  return message;
}

// define route handlers
console.log("Configuring route handlers");
router.handle("GET", "/hello", (request, response, context) => {
  console.log("Received request");
  response.send(200, getMessage());
});

// set the port to listen on and register a callback
config.port = 3000;
config.onBind(() => {
  console.log("Server listening! 🚀");
});

// specific options for the Netty backend
console.log("configuring other stuff")
config.transport = "nio";

// we're done!
console.log("✨ Configuration finished! ✨");