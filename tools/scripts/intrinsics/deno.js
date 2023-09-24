// region Definitions

/* Simple class mimicking Deno's HTTP Response type. */
class Response {
  /* Status code for this response. */
  status;

  /* Content to be sent with this response. */
  content;

  constructor(content = "", status = 200) {
    this.status = status;
    this.content = content;
  }
}

/* Private implementation of the Deno.serve function wrapped around Elide's HTTP intrinsics. */
function _deno_serve(options) {
  // resolve http intrinsics
  const server = Elide.http;
  const router = server.router;
  const config = server.config;

  // apply basic options (port, etc.)
  // support for 'hostname' and 'signal' is under development
  config.port = options.port || 8080;

  // register the callback if requested
  if (options.onListen) config.onBind(() => options.onListen({
    port: config.port,
    hostname: options.hostname || "0.0.0.0"
  }));

  // now wrap the handler (null for both 'method' and 'path' means match all requests)
  router.handle(null, null, (request, response, context) => {
    // in Deno, handlers return a response object rather than sending an existing one
    const denoResponse = options.handler(request);
    
    // translate from the Deno Response object and send the response
    response.send(denoResponse.status, denoResponse.content);
  });
  
  // start the server
  server.start()
}

const Deno = {
  serve: _deno_serve
};

// endregion
// ------------------------------------------------------------------------------------------------


// usage example
Deno.serve({
  port: 3000,
  hostname: "0.0.0.0",
  handler: (_req) => new Response("Hello from 'Deno'! 😉"),
  onListen({ port, hostname }) {
    console.log(`'Deno' server started at http://${hostname}:${port}`);
  }
});