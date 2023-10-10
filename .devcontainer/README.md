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

**To verify working installation:**
```
elide selftest
```

### JavaScript

**To run a JavaScript terminal:**
```
elide shell
```

**Encode some JSON:**
```javascript
JSON.stringify({x: 5})
```

**To see `.env` environment:**
```python
JSON.stringify(process.env)
```

**Generate a UUID:**
```javascript
crypto.randomUUID()+""
```

**Parse a URL and print the host:**
```javascript
new URL("https://google.com").host
```

**To run a JavaScript server:**
```
elide serve tools/scripts/server.js
```

### Python

**To run a Python terminal:**
```
elide shell --python
```

**To see `.env` environment:**
```python
import os; os.environ
```
