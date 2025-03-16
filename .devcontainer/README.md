<p align="center">
  <a href="https://github.com/elide-dev">
    <img src="https://static.elide.dev/assets/org-profile/creative/elide-banner-purple.png" alt="Elide" height=240 />
  </a>
</p>

## Elide + Codespaces

This GitHub Codespace comes pre-installed with the latest version of [Elide](https://elide.dev).

## Stuff to try

**Your terminal should already be open at the bottom of your screen.** We have already taken care of running the first command, `elide --help`, for you. Here's some more stuff you can try.

### Basic commands

**To see runtime info:**

```
elide info
```

### JavaScript

**To run a JavaScript terminal:**

```
elide
```

**Encode some JSON:**

```javascript
JSON.stringify({ x: 5 });
```

**To see `.env` environment:**

```python
JSON.stringify(process.env)
```

**Generate a UUID:**

```javascript
crypto.randomUUID() + "";
```

**Parse a URL and print the host:**

```javascript
new URL("https://google.com").host;
```

**To run a JavaScript server:**

```
elide serve tools/scripts/server.js
```

**Elide can run TypeScript directly:**

```typescript
const x: number = 5;
console.log("the number is: " + x);
```
```
> elide ./sample.ts
```
```
the number is 5
```

### Python

**To run a Python terminal:**

```
elide python
```

**To see `.env` environment:**

```python
import os; print(os.environ)
```

**To see all environment, including host env:**

```
elide run --python --host:allow-env
```

```python
import os; print(os.environ)
```

### Language Interop

Create a Python file:
```python
# sample.py
def say_hello(name = "Python"):
  return f"Hello, {name}!"
```

Create a TypeScript file:
```typescript
// sample.ts
import py from "./sample.py";

console.log(`${py.say_hello()} + TypeScript!`);
```

Run it:

```
> elide ./sample.ts
```

```
Hello, Python + TypeScript!
```

## Further Reading

Check out Elide's [docs](https://docs.elide.dev) for more exercises.
