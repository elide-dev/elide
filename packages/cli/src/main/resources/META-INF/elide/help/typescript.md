# Elide + TypeScript

Elide supports TypeScript as part of the runtime itself. This means you can:

- Run TypeScript code directly, with no build step
- Use TypeScript's type system to check your code at compile time
- Build and export TypeScript types as part of your project

## TSX

Additionally, Elide can pre-process your TSX components. Use TSX where you want to, and add React depedencies to your
app as normal (see `elide help react`). Elide can't use JSX intrinsics from other libraries yet.

Here's a TSX sample:

**`sample.tsx`**
```tsx
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
```

Running this is easy and requires no build step:

```bash
> elide ./sample.tsx
<body><h1>Hello from script!</h1></body>
```

> [!NOTE]
> See `elide help react` for more information about using React with Elide.
