"""Defines the export surface for code used from TypeScript rules."""

load(
    "//tools/defs/tsc:typescript.bzl",
    _ts_config = "ts_config",
    _ts_library = "ts_library",
    _ts_project = "ts_project",
)

## Exports.
ts_config = _ts_config
ts_library = _ts_library
ts_project = _ts_project
