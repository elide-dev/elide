// retrieve the binding from global symbols
console.log("Resolving router intrinsics");
const router = Elide.http.router;

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
  response.send(200, getMessage());
});

// we're done!
console.log("✨ Configuration finished! ✨");