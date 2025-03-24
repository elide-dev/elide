import { renderToString } from "react-dom/server"

function Component(props: { message: string }) {
  return (
    <body>
      <h1>{props.message}</h1>
    </body>
  )
}

async function render(): Promise<string> {
  return await renderToString(<Component message="Hello from script!" />)
}

console.log(await render())
