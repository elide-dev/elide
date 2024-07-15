# Contributing to Elide

The Elide team welcomes new contributors :) and we are generally easy to find on Discord, GitHub, or Slack. See the [support](./SUPPORT.md) page for links.
The following guide should help you get started and submit a PR.

## Building Elide

Follow the directions in the main [README](../README.md). You will need a recent copy of GraalVM for Java 20, and several other components.
Once you have these installed, you can try the `Makefile`:

### Notable tasks

- `make info`: Show build environment
- `make build`: Build all targets
- `make test`: Build all targets, run all tests
- `make check`: Build all targets, run all tests, run all checks
- `make native`: Build the native binary for the CLI
- `make natives`: Rebuild native dependencies
- `make help`: Show all make tasks

### All tasks

```
$ make help
Elide:
api-check                      Check API/ABI compatibility with current changes.
build                          Build the main library, and code-samples if SAMPLES=yes.
check                          Build all targets, run all tests, run all checks.
clean-cli                      Clean built CLI targets.
clean-docs                     Clean documentation targets.
clean-natives                  Clean local native targets.
clean                          Clean build outputs and caches.
cli-local                      Build the Elide command line tool and install it locally (into ~/bin, or LOCAL_CLI_INSTALL_DIR).
cli-release                    Build an Elide command-line release.
cli                            Build the Elide command-line tool (native target).
dependency-packages            Print the suite of dependencies to install for this OS.
distclean                      DANGER: Clean and remove any persistent caches. Drops changes.
docs                           Generate docs for all library modules.
fmt                            Run all formatter tools.
forceclean                     DANGER: Clean, distclean, and clear untracked files.
format                         Alias for `make fmt`.
gvm                            Build a custom copy of GraalVM for use locally.
help                           Show this help text ('make help').
image-base-alpine              Build base Alpine image.
image-base                     Build base Ubuntu image.
image-gvm17                    Build GVM17 builder image.
image-gvm21                    Build GVM21 builder image.
image-gvm22                    Build GVM22 builder image.
image-jdk17                    Build JDK17 builder image.
image-jdk21                    Build JDK21 builder image.
image-jdk22                    Build JDK20 builder image.
image-native-alpine            Build native Alpine base image.
image-native                   Build native Ubuntu base image.
image-runtime-jvm17            Build runtime GVM17 builder image.
image-runtime-jvm21            Build runtime GVM21 builder image.
image-runtime-jvm22            Build runtime GVM22 builder image.
images                         Build all Docker images.
info                           Show info about the current codebase and toolchain.
model-update                   Update the proto model and re-build it.
model                          Build proto model targets.
native                         Build Elide's native image target; use BUILD_MODE=release for a release binary.
natives-coverage               Show the current native coverage report; only run if `natives-test` is run first.
natives-test                   Run Cargo and native tests, optionally buildin coverage if COVERAGE=yes.
natives                        Rebuild natives (C/C++ and Rust).
publish                        Publish a new version of all Elide packages.
release                        Perform a full release, including publishing to Maven Central and the Elide repository.
relock-deps                    Update dependency locks and hashes across Yarn and Gradle.
reports                        Generate reports for tests, coverage, etc.
runtime-build                  Build the JS runtime facade and the builtin modules bundle
runtime-update                 Rebuild and copy the JS runtime facade
runtime                        Build and update the JS runtime if needed.
serve-docs                     Serve documentation locally.
setup                          Setup development pre-requisites.
test                           Run the library testsuite, and code-sample tests if SAMPLES=yes.
third-party                    Build all third-party embedded projects.
umbrella                       Build the native umbrella tooling library.
update-deps                    Perform interactive dependency upgrades across Yarn and Gradle.
update-jdeps                   Interactively update Gradle dependencies.
update-jsdeps                  Interactively update Yarn dependencies.
```

You can build from the `Makefile`:

```
$ make build
```

And run the tests, as long as you are building with GraalVM:

```
$ make test
```

## Filing a Pull Request

We use [GitHub Issues][1] and Pull Requests to conduct the code review process. Here are some guidelines about how we approach this:

1. **File as a draft to start.**
   We will wait to review your PR until it is out of draft (unless you request early feedback). Feel free to push and force-push here as needed.

2. **Add tests for your change.**
   Ideally, your change should have close to 100% test coverage. In some cases, we accept changes with no coverage (for urgent hotfixes, etc).

3. **When ready, request review.**
   GitHub is configured to require certain workflows to pass, and to require review from code owners. You will need at least one passing review in order
   to merge your change.

4. **Team merges change.**
   Once your change passes all tests and checks, and has been reviewed, a member of the Elide team can add it to the merge queue.

## PR Feedback Tools

Each Pull Request runs a suite of static analyzers, dependency checks, and other tools. You should use these tools to gather feedback
on your change and amend accordingly.

**Tools which run on PRs:**

- `ktlint` is configured as a linter. Your change should pass a `ktlintCheck` job.
- The Kotlin API validator runs on each PR to ensure we don't break API consumers. You can update API pins with `apiDump` (on the applicable project).
- We will not accept dependencies with medium or high-severity vulnerabilities

## Reporting bugs

We use [GitHub Issues](https://github.com/elide-dev/elide/issues/new/choose) to track bugs and requests. Please check current issues before submitting to avoid duplicate reports.

## Feature requests and discussions

If your contribution adds a new feature or makes substantial changes to the code, please [start a new GitHub discussion](https://github.com/orgs/elide-dev/discussions/new/choose) explaining the motivation and scope of such changes. You can also reach out through our [Discord server](https://elide.dev/discord) and share your ideas there.

## Contributing with pull requests

If you are opening a Pull Request, you may want to add a quick summary of the changes in the description, as well as add comments in relevant places in the code. This will help our maintainers during code review and increase the chance of the changes being merged.

Opening "draft" Pull Requests is a good idea if you intend to continue working on your branch but still need the feedback provided by automated checks.

#### Conventional commits

We use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) to maintain standard commit messages, as well as for Pull Request and Issues.

#### Document your code

Make sure to add documentation comments for your code, especially if it includes public methods or classes that can be exposed to users. The standard guidelines for Kotlin doc comments apply, and we encourage the use of `@param`, `@return`, and `@throws` labels.

#### Write tests

Including tests for your changes is highly recommended, they will run automatically using GitHub Actions, and can provide you with quick feedback even before code review. High code coverage is welcome but not required, as long as the tests cover all relevant changes.

#### Code Review and automated checks

All Pull Requests need to be reviewed by maintainers of the repository before they can be merged. They will also need to pass a series of automated checks (via GitHub Actions), including unit tests, API compatibility checks, and code quality inspection.

Once you are satisfied with your work and want to submit it for review, mark it as "Ready", and a maintainer will take a look to decide whether it will be merged or not, or request additional changes to comply with contribution guidelines.

## Build environment

The following tools are required for local development:

- [Gradle 8.4](https://gradle.org/releases/) or newer, using the Gradle wrapper will automatically download the correct distribution the first time a task is executed.
- The latest [GraalVM JDK](https://www.graalvm.org/downloads/) distribution, the `native-image` component is required to build the native CLI distribution.

When working on certain parts of the repository, such as the code samples, you may need to set the value for some properties in your local properties file at `~/.gradle/gradle.properties`:

```properties
# without this, sample projects will not be included in the build
buildSamples = true
```

Check the `gradle.properties` file at the project root to see all available options.

## Building a custom runtime

If you are building a JVM application that runs guest code in one of the languages supported by Elide, you can use the Runtime DSL to configure your own embedded polyglot engine:

```kotlin
implementation("dev.elide:elide-graalvm:1.0.0-alpha10")
```

or for Groovy scripts:

```groovy
implementation 'dev.elide:elide-graalvm:1.0.0-alpha10'
```

The DSL is used internally by the Elide binaries and by the SSR packages and provides a simplified API to harness the power of the underlying [GraalVM](https://graalvm.org) engine:

```kotlin
// configure and create a new engine
val engine = PolyglotEngine {
  // add support for the JavaScript language
  install(JavaScript) {
    // enable ESM support
    esm = true

    // include custom bindings into the engine, these
    // will be available as top level symbols
    bindings {
      put("version", "1.0.0")
    }
  }
}

// obtain a context to evaluate source code
val context = engine.acquire()

// evaluate guest JavaScript code
context.javascript("""
  console.log(`Hello from custom runtime v${version}`)
""")
```

> [!WARNING]
> The Elide Runtime DSL is intended for advanced use cases, and while we try not to break compatibility between releases, there are no guarantees.
