
"""Internal Bazel definitions and macros for use in this codebase only."""

load(
    "@elide//tools/defs/closure:externs.bzl",
    _closure_extern = "closure_extern",
    _extern = "extern",
    _js_extern = "js_extern",
)
load(
    "@elide//tools/defs:elide.bzl",
    _js_library = "js_library",
    _ts_library = "ts_library",
    _ts_config = "ts_config",
    _elide_test = "elide_test",
)

## Closure: Externs.
closure_extern = _closure_extern
extern = _js_extern
js_extern = _js_extern

## Elide: Macros.
js_library = _js_library
ts_library = _ts_library
ts_config = _ts_config

## Elide: Rules.
elide_test = _elide_test
