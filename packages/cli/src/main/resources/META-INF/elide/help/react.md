# Elide + React

Elide provides integration with React at the runtime layer; to enable this integration, install React as normal in your
project, and then follow the directions below.

## Installing React

Install React using `elide.pkl`, or `package.json` and your favorite dependency manager. Using `elide.pkl`:
```pkl
// ...

dependencies {
  npm {
    packages {
      "react@18"
      "react-dom@18"
    }
  }
}
```

Using `package.json` and your favorite installer:
```json
{
  "dependencies": {
    "react": "18",
    "react-dom": "18"
  }
}
```

Then run `elide install` or `<npm|pnpm|yarn|bun|...> install`.

> [!NOTE]
> Elide doesn't support React 19 yet.

## Using React

You can import and use React as normal, using CJS or ESM, both of which Elide supports. You can use JSX and TSX simply
by using the `.jsx` or `.tsx` file extensions:

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
> See `elide help typescript` for more information about TypeScript integration.
