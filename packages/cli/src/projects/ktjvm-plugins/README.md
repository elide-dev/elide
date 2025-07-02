# Elide Sample: Kotlin/JVM with Plugins

Sample project which demonstrates a Kotlin/JVM project built using Elide, while also using KotlinX Serialization, which
is a plugin for the Kotlin compiler.

See [`elide.pkl`](./elide.pkl) for a configuration sample:
```pkl
// ...
kotlin {
  features {
    // Activates KotlinX serialization support, including plugins and dependencies.
    serialization = true
  }
}
```
