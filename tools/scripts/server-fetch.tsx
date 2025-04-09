/// <reference path="../../packages/types/index.d.ts" />
// noinspection ES6UnusedImports

import * as React from "react"
import { renderToString } from "react-dom/server"

function Component(props: { message: string }) {
  return (
    <body>
      <h1>{props.message}</h1>
    </body>
  )
}

async function plaintext(): Promise<Response> {
  return new Response("Hello, Elide!")
}

async function react(): Promise<Response> {
  return new Response(await renderToString(<Component message="Hello from server!" />))
}

async function json(): Promise<Response> {
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
  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url)
    const handler = handlers[`${request.method} ${url.pathname}`]
    if (!handler) return new Response(null, { status: 404 })
    return await handler(request)
  },
}
