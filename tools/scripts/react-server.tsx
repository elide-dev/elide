/// <reference path="../../packages/types/index.d.ts" />
import { renderToString } from "react-dom/server"

const port = 8080

function Component(props: { message: string }) {
  return (
    <body>
      <h1>{props.message}</h1>
    </body>
  )
}

async function render(): Promise<string> {
  return await renderToString(<Component message="Hello from server!" />)
}

if (!Elide.http) {
  throw new Error("Running under Elide but no server is available: please run with `elide serve`")
}
Elide.http.router.handle("GET", "/", async (_, resp) => {
  resp.header("Content-Type", "text/html")
  resp.send(200, await render())
})

// configure the server binding options
Elide.http.config.port = port

// receive a callback when the server starts
Elide.http.config.onBind(() => {
  console.log(`Server listening at "http://localhost:${port}"! 🚀`)
})

// start the server
Elide.http.start()
