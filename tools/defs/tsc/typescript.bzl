"""Provides macros for compiling TypeScript to Closure JS."""

load(
    "@npm//@bazel/typescript:index.bzl",
    _ts_config = "ts_config",
    _ts_project = "ts_project",
)
load(
    "@npm//@bazel/typescript:index.bzl",
    _ts_library = "ts_library",
)
load(
    "//tools/defs/closure:exports.bzl",
    _closure_js_library = "closure_js_library",
)
load(
    "//tools/defs/tsc:consumer.bzl",
    _ts_consumer = "devmode_consumer",
)

ES_TARGET = "es2020"
MODULE_TARGET = "esnext"

_BASE_TS_DEPS = [
    # Nothing at this time.
]

_BASE_TS_TOOLS = [
    "//elide/runtime/js/intrinsics:base",
    "//elide/runtime/js/intrinsics:primordials",
]

_BASE_JS_DEPS = [
    "//third_party/google/tsickle:tslib",
    "//elide/runtime/js:runtime_externs",
]

_BASE_SUPPRESSIONS = [
    "JSC_UNREACHABLE_CODE",
]

_BASE_TS_ARGS = {
    "devmode_module": MODULE_TARGET,
    "devmode_target": ES_TARGET,
    "prodmode_module": MODULE_TARGET,
    "prodmode_target": ES_TARGET,
    "compiler": "//tools/defs/tsc/compiler:tsc_wrapped",
    "tsconfig": "//elide/runtime/js:tsconfig",
}

def _fixup_shortlabel(label):
    """Fixup a short-label which has no target."""
    if label.startswith("@"):
        return label
    elif ":" not in label:
        return (label + ":" + (label.split("/")[-1]))
    else:
        return label

def _wrapped_ts_library(
        name,
        module,
        srcs = [],
        deps = [],
        closure_deps = [],
        lib_kwargs = {},
        suppress = [],
        include_tools = True,
        nowrap = False,
        *args,
        **kwargs):
    """Wrap `ts_library` with extra arguments, which are considered standard for Elide's runtime TS environment."""

    config = {}
    config.update(_BASE_TS_ARGS)
    config.update(kwargs)

    native.filegroup(
        name = "%s_src" % name,
        srcs = srcs,
    )

    resolved_deps = []

    if nowrap:
        _ts_library(
            name = "%s_ts" % name,
            module_name = module,
            srcs = srcs and [":%s_src" % name] or [],
            deps = deps,
            *args,
            **config
        )
    else:
        ts_deps_resolved = _BASE_TS_DEPS + ["%s_ts" % _fixup_shortlabel(i) for i in deps]
        closure_deps_resolved = ["%s_js" % _fixup_shortlabel(i) for i in closure_deps]
        if include_tools:
            ts_deps_resolved += _BASE_TS_TOOLS

        _ts_library(
            name = "%s_ts" % name,
            module_name = module,
            srcs = srcs and [":%s_src" % name] or [],
            deps = ts_deps_resolved,
            *args,
            **config
        )
        resolved_deps = closure_deps_resolved

    _ts_consumer(
        name = "%s_consumer" % name,
        deps = [":%s_ts" % name],
    )
    _closure_js_library(
        name = "%s_js" % name,
        srcs = [":%s_consumer" % name],
        deps = _BASE_JS_DEPS + resolved_deps,
        suppress = _BASE_SUPPRESSIONS + (suppress or []),
        **lib_kwargs
    )
    native.alias(
        name = name,
        actual = "%s_ts" % name,
    )

ts_library = _wrapped_ts_library
ts_config = _ts_config
ts_project = _ts_project
