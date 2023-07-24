"""Provides macros and other definitions for working with Closure Externs."""

load(
    "//tools/defs/closure:exports.bzl",
    _closure_js_library = "closure_js_library",
)

def js_extern(name, srcs = [], deps = [], repo = "@externs", **kwargs):
    """Define a well-known extern from the latest Closure Compiler source code."""

    ## define file-group for the extern source
    native.filegroup(
        name = "%s_src" % name,
        srcs = srcs,
        visibility = ["//visibility:private"],
    )

    ## wrap as closure library
    _closure_js_library(
        name = "%s_js" % name,
        srcs = [":%s_src" % name],
        deps = deps,
        visibility = ["//visibility:public"],
        **kwargs
    )

    ## alias the main extern name to the JS lib
    native.alias(
        name = name,
        actual = "%s_js" % name,
    )

def closure_extern(path):
    """Calculate the path to a named Closure Compiler extern."""
    return "%s.js" % path

def extern(name, repo = "@externs", variant = ""):
    """Calculate a well-known extern name from the provided inputs."""

    return "%s//:%s%s" % (
        repo,
        name.replace("/", "_"),
        variant,
    )
