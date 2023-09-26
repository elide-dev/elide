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