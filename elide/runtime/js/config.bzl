load(
    "//tools/defs/closure:externs.bzl",
    "extern",
)

## JS: Debugging
## -------------
## Whether to build debugging features into the runtime.
DEBUG = False

## JS: Language Level
## ------------------
## Language level to use for the compiled runtime.
JS_LANGUAGE = "ECMASCRIPT_2021"

## JS: Language Target
## -------------------
## Effective language level for the final compiled runtime.
JS_TARGET = "es2021"

## JS: Compiler Arguments
## ----------------------
## Configures the Closure Compiler.
JS_ARGS = []

## JS: Intrinsics & Externs
## ------------------------
## Describes the baseline set of externs available in the Elide JS runtime environment.
BASE_JS_EXTERNS = [extern(i) for i in [
    "browser/intl",
]] + [
    "//third_party/graalvm/js",
    "//third_party/standards/whatwg",
]

## JS: Static Defines
## ------------------
## Compile-time definitions which control code inclusion and behavior.
DEFINES = {}

## TS: Runtime Modules Package
## ---------------------------
## Specifies the package where TypeScript sources are located for JS intrinsics.
TS_MODULE_PACKAGE = "//elide/runtime/js/intrinsics"

## TS: Runtime Modules
## -------------------
## Registers each TypeScript module used in the TS/JS runtimes.
TS_MODULES = ["%s:%s" % (TS_MODULE_PACKAGE, t) for t in [
    # Top-level modules.
    "base64",
    "console",
]] + ["%s/%s:%s" % (TS_MODULE_PACKAGE, t, t) for t in [
    # Sub-modules
    "blob",
    "err",
    "url",
]] + [
    # General Modules
    "//elide/runtime/js/bridge:js-error",
]
