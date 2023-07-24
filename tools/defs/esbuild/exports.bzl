"""Exports symbols from the ESBuild rules for Bazel."""

load(
    "@npm//@bazel/esbuild:index.bzl",
    _esbuild = "esbuild",
    _esbuild_config = "esbuild_config",
)

## Exports
esbuild = _esbuild
esbuild_config = _esbuild_config
