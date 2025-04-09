/// <reference path="../../packages/types/index.d.ts" />
// noinspection ES6UnusedImports

import * as React from "react"
import { renderToString } from "react-dom/server"

function Component(props) {
  return (
    <body>
      <h1>{props.message}</h1>
    </body>
  )
}

async function plaintext() {
  return new Response("Hello, Elide!")
}

async function react() {
  return new Response(await renderToString(<Component message="Hello from server!" />))
}

async function json() {
  const headers = new Headers()
  headers.set("Content-Type", "application/json")
  return new Response(JSON.stringify({ message: "Hello, World!" }), {
    headers,
  })
}

const handlers = {
  [`GET /`]: plaintext,
  [`GET /plaintext`]: plaintext,
  [`GET /json`]: json,
  [`GET /react`]: react,
}

// noinspection JSUnusedGlobalSymbols
export default {
  async fetch(request) {
    const url = new URL(request.url)
    const handler = handlers[`${request.method} ${url.pathname}`]
    if (!handler) return new Response(null, { status: 404 })
    return await handler(request)
  },
}
