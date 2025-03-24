/// <reference types="bun-types" />
import { renderToString } from "react-dom/server"

const port = 8080

function Component(props) {
  return (
    <body>
      <h1>{props.message}</h1>
    </body>
  )
}

async function render() {
  return await renderToString(<Component message="Hello from server!" />)
}

if (globalThis.Elide) {
  console.log(`Serving via Elide (port: ${port})`)
  if (!Elide.http) {
    throw new Error("Running under Elide but no server is available: please run with `elide serve`")
  }
  Elide.http.router.handle("GET", "/", async (_, resp) => {
    resp.header("Content-Type", "text/html")
    resp.send(200, await render())
  })
} else {
  console.log(`Serving via Bun (port: ${port})`)
  Bun.serve({
    port,
    async fetch() {
      return new Response(await render(), {
        headers: { "Content-Type": "text/html" },
      })
    },
  })
}
