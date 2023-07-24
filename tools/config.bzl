load(
    "//tools/defs/elide:bindist.bzl",
    _LATEST_ELIDE_VERSION = "latest_version",
)

## Runtime version tag.
## ------------------------------------
VERSION = "1.0.0-beta4"

## Elide CLI version.
## ------------------------------------
## Pinned version of the CLI for testing.
ELIDE_VERSION = _LATEST_ELIDE_VERSION

## `Debug` mode.
## ------------------------------------
## Set to `True` to shut off symbol rewriting and enable logging.
DEBUG = False

## Closure JS target.
## ------------------------------------
## Describes the language level we should output JS in.
OUTPUT_TARGET = "ECMASCRIPT_2019"

## `CHROMIUM` enable/disable flag.
## ------------------------------------
## Set to `True` to run tests on Chromium via WebDriver.
CHROMIUM = True

## `FIREFOX` enable/disable flag.
## ------------------------------------
## Set to `True` to run tests on Firefox via WebDriver.
FIREFOX = True

## `SAUCE` enable/disable flag.
## ------------------------------------
## Set to `True` to run tests on SauceLabs, as configured.
SAUCE = True

## GraalVM version.
## ------------------------------------
## Assigned to the latest available CE VM.
GRAALVM_VERSION = "22.3.0"

## GraalVM JDK version.
## ------------------------------------
## Specifies the version of the underlying VM JDK.
GRAALVM_JDK_VERSION = "17"

## Java version.
## ------------------------------------
## Sets the language level for JVM output.
JAVA_LANGUAGE_LEVEL = "11"

## Kotlin language version.
## ------------------------------------
## Sets the Kotlin API version.
KOTLIN_LANGUAGE_LEVEL = "1.7"

## Kotlin SDK version.
## ------------------------------------
## Sets the Kotlin runtime version.
KOTLIN_SDK_VERSION = "1.7.22"

## Kotlin compiler version.
## ------------------------------------
## Sets the Kotlin compiler version to use.
## Repo to query: `@com_github_jetbrains_kotlin`
KOTLIN_COMPILER_VERSION = KOTLIN_SDK_VERSION

## Kotlin compiler fingerprint.
## ------------------------------------
## SHA-256 from JetBrains' Kotlin release page.
KOTLIN_COMPILER_FINGERPRINT = None

## Protobuf toolchain version.
## ------------------------------------
## Sets the version enforced throughout for Protobuf.
PROTOBUF_VERSION = "3.20.1"

## JVM-based app debug port.
## ------------------------------------
## Sets the port to wait/listen for remote JVM tools on (for launching a debugger).
JVM_DEBUG_PORT = "5005"

## Golang version to use.
## ------------------------------------
## Sets the version to use for Google's Go language.
GO_VERSION = "1.18"

## NodeJS version to use.
## ------------------------------------
## Sets the version of the Node JS runtime to use.
NODE_VERSION = "16.6.2"

## Yarn version to use.
## ------------------------------------
## Pins the version for the Yarn package manager for Node.
YARN_VERSION = "1.22.19"

## Buf version to use.
## ------------------------------------
## Pins the version of the Buf toolchain to use.
BUF_VERSION = "v1.5.0"

## Rust: Edition to use.
## ---------------------
## Sets the edition of the Rust language to use.
RUST_EDITION = "2018"

## Rust: Version to use.
## ---------------------
## Sets the version of the Rust SDK to use.
RUST_VERSION = "1.66.0"

## JS: Enable J2CL
## ---------------
## Enable integration with the J2CL toolchain.
ENABLE_J2CL = True

## JS: Enable J2WASM
## -----------------
## Enable integration with the J2WASM toolchain.
ENABLE_J2WASM = False

## Elide: Use Local CLI
## --------------------
## Whether to use a local CLI copy.
LOCAL_ELIDE = False
