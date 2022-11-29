## Tools: Bundler

This is where code will eventually live which performs the actual build steps for bundling an Elide application.
Concrete plug-ins in each ecosystem (Bazel, Gradle, etc.) should then call into these tools, once available.
