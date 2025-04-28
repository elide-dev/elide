# Elide Projects

This guide explains how projects work in Elide. Projects are a way to organize your code and manage dependencies, build
tasks, and other stuff centrally.

- Use dependencies from *Maven*, *NPM*, *PyPI*, *Rubygems*, *HuggingFace*, and others
- Organize source code, and tasks to build/package it
- Declare and configure arbitrary tasks in their build graph
- Define and configure artifacts built by the project
- Gather and run tests

## Getting started

To create a new project interactively, run:
```console
elide init
```

Or, create an `elide.pkl` file in the root of the project:
```pkl
amends "elide:project.pkl"

name = "my-project"
```

### Dependencies

To add dependencies, use the `dependencies` block:
```pkl
amends "elide:project.pkl"

name = "my-project"

dependencies {
  maven {
    packages {
      "com.google.guava:guava"
    }
  }
  npm {
    packages {
      "react@18"
    }
    devPackages {
      "typescript"
    }
  }
}
```

Dependencies are installed automatically when needed, and cached for later use.
You can install manually with `elide install`.

### Scripts

To add scripts, use the `scripts` block:

```pkl
amends "elide:project.pkl"

name = "my-project"

scripts {
  ["sample"] = "echo 'Hello script'"
}
```

With the above `script` defined, you can run it with:
```console
elide sample
```

Scripts also work with `package.json` and other manifest types.

### Using existing manifests

Elide might be able to use your existing project manifest:

| Ecosystem | Manifest           | Dependencies | Tasks | Notes     |
|-----------|--------------------|--------------|-------|-----------|
| Python    | `pyproject.toml`   | ✅            | ✅     | Supported |
| Python    | `requirements.txt` | ✅            | N/A   | Supported |
| NPM       | `package.json`     | ✅            | ✅     | Supported |

#### What does 'supported' mean?

- **Dependencies**: Elide can read and install dependencies.
- **Tasks**: Elide can read tasks (or scripts, as applicable) and run them.

## Usage

To interact with your project, defined by `elide.pkl` or another manifest:
```console
elide project
```

To build the project:
```console
elide build
```

To test the project:
```console
elide test
```
