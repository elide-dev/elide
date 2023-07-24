"""Defines the export surface for code used from Closure and the Closure Rules for Bazel."""

load(
    "@rules_java//java:defs.bzl",
    _java_binary = "java_binary",
    _java_library = "java_library",
)
load(
    "//tools/defs/java:jarbuilder.bzl",
    _jar_resources = "jar_resources",
)

## Exports.
java_library = _java_library
jar_resources = _jar_resources
