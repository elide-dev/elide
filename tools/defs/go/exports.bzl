"""Exports the API surface used from the Go Rules for Bazel."""

load(
    "@rules_go//go:def.bzl",
    _go_binary = "go_binary",
    _go_library = "go_library",
)

## Exports.
go_library = _go_library
go_binary = _go_binary
