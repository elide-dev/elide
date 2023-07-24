"""Macros for special types of JARs."""

load(
    "@rules_java//java:defs.bzl",
    _java_library = "java_library",
)
load(
    "//tools/defs/pkg:exports.bzl",
    _pkg_files = "pkg_files",
    _pkg_zip = "pkg_zip",
)

def _jar_resources(name, language, srcs = [], manifest = None, zip_args = {}, strip_prefix = None, *args, **kwargs):
    """Package the provided `srcs` into a resources-only JAR, optionally with the provided `manifest`."""

    if manifest:
        _pkg_files(
            name = "%s-zip-manifest" % name,
            srcs = [manifest],
            strip_prefix = strip_prefix,
            prefix = "META-INF/",
        )

    _pkg_zip(
        name = "%s-zip" % name,
        srcs = srcs,
        out = "%s.zip" % name,
        strip_prefix = strip_prefix,
    )

    _pkg_files(
        name = "%s-jar-runtime" % name,
        srcs = srcs,
        strip_prefix = strip_prefix,
        prefix = "elide/runtime/%s/" % language,
    )

    _pkg_zip(
        name = "%s-jar" % name,
        srcs = [":%s-jar-runtime" % name] + ([":%s-zip-manifest" % name] if manifest else []),
        out = "%s.jar" % name,
    )

    _java_library(
        name = "%s-lib" % name,
        resources = srcs,
        *args,
        **kwargs
    )

    native.filegroup(
        name = "%s-sources" % name,
        srcs = [
            "%s-lib" % name,
            "%s-jar" % name,
            "%s-zip" % name,
        ],
    )

    native.alias(
        name = name,
        actual = "%s-sources" % name,
    )

## Exports.
jar_resources = _jar_resources
