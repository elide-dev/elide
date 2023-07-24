"""Macros and definitions for defining Elide Runtime targets."""

load(
    "//elide/runtime/js:config.bzl",
    _DEBUG = "DEBUG",
    _DEFINES = "DEFINES",
    _JS_ARGS = "JS_ARGS",
    _JS_LANGUAGE = "JS_LANGUAGE",
    _JS_TARGET = "JS_TARGET",
)
load(
    "//tools/defs/closure:exports.bzl",
    _closure_js_binary = "closure_js_binary",
    _closure_js_library = "closure_js_library",
)
load(
    "//tools/defs/pkg:exports.bzl",
    _pkg_filegroup = "pkg_filegroup",
    _pkg_files = "pkg_files",
    _pkg_tar = "pkg_tar",
    _pkg_zip = "pkg_zip",
)
load(
    "//tools/defs/java:exports.bzl",
    _jar_resources = "jar_resources",
)
load(
    "//tools/defs/esbuild:exports.bzl",
    _esbuild = "esbuild",
    _esbuild_config = "esbuild_config",
)
load(
    "//tools/defs/tsc:exports.bzl",
    _ts_config = "ts_config",
    _ts_library = "ts_library",
    _ts_project = "ts_project",
)
load(
    "//tools/defs/elide:elide.bzl",
    _elide_test = "elide_test",
)

_RUNTIME_JS_ARGS = _JS_ARGS + [
    # Additional Closure Compiler arguments for the runtime.
    "--env=CUSTOM",
    "--inject_libraries=true",
    "--rewrite_polyfills=true",
]

_JS_MODULES_CLOSURE = False

_RUNTIME_DEFINES = {}
_RUNTIME_DEFINES.update(_DEFINES)
_RUNTIME_DEFINES.update({
    # Additional runtime definitions here.
})

# Module prefix to apply for JS runtime "modules".
_JS_MODULE_PREFIX = "@elide/runtime/module"

_common_js_library_config = {
    "language": _JS_LANGUAGE,
}

_base_js_library_config = {
    "convention": "CLOSURE",
}

_base_js_runtime_config = {
    "language": _JS_LANGUAGE,
    "dependency_mode": "PRUNE",
    "output_wrapper": "(function(){%output%}).call({});",
}

_ts_compiler_args = {
    # Nothing at this time.
}

def _abstract_runtime_targets(name, srcs = [], deps = [], **kwargs):
    """Create any abstract targets required by each concrete target."""
    native.filegroup(
        name = name,
        srcs = srcs,
        visibility = ["//visibility:private"],
    )

def _js_library(name, srcs = [], deps = [], ts_deps = [], exports = [], **kwargs):
    """Create a library target for code which implements some aspect of Elide JS runtime functionality."""

    if len(srcs) == 0 and len(exports) > 0:
        _closure_js_library(
            name = name,
            srcs = srcs,
            exports = exports,
            **kwargs
        )
    else:
        _abstract_runtime_targets(
            name = "%s_src" % name,
            srcs = srcs,
            deps = deps,
            **kwargs
        )
        config = {}
        config.update(_common_js_library_config)
        config.update(_base_js_library_config)
        config.update(kwargs)

        deplist = [i for i in deps]
        deplist += ["%s_js" % i for i in ts_deps]

        _closure_js_library(
            name = "%s_js" % name,
            srcs = [":%s_src" % name],
            deps = deplist,
            **config
        )
        native.alias(
            name = name,
            actual = ":%s_js" % name,
        )

def _js_module(
        name,
        package_json = "package.json",
        entry_point = None,
        module = None,
        js_srcs = [],
        srcs = [],
        deps = [],
        ts_deps = [],
        ts_args = {},
        js_args = {},
        **kwargs):
    """Defines a JavaScript module target."""

    outs = []
    jsouts = []
    module_path = "%s/%s" % (_JS_MODULE_PREFIX, module or name)
    compiler_args = [] + _RUNTIME_JS_ARGS

    if entry_point == None:
        default_ext = len(srcs) > 0 and ".ts" or ".js"
        entry_point = "index%s" % default_ext

    if not _JS_MODULES_CLOSURE:
        _esbuild(
            name = "%s.jsopt" % name,
            srcs = js_srcs + srcs,
            entry_point = entry_point,
            format = "esm",
            output = "%s.mjs" % (module or name),
            sourcemap = "external",
            target = _JS_TARGET,
        )
        native.filegroup(
            name = "%s_module_src" % name,
            srcs = [
                package_json,
                "%s.mjs" % name,
            ],
        )
    else:
        if len(srcs) > 0:
            _ts_library(
                name = "%s_ts" % name,
                srcs = srcs,
                deps = ts_deps,
                module = "/".join(module_path.split("/")[0:-1]),
                **ts_args
            )
            native.filegroup(
                name = "%s_types" % name,
                srcs = [":%s_ts" % name],
                output_group = "types",
            )
            native.filegroup(
                name = "%s_tsdev" % name,
                srcs = [":%s_ts" % name],
                output_group = "es5_sources",
            )
            native.filegroup(
                name = "%s_tsprod" % name,
                srcs = [":%s_ts" % name],
                output_group = "es6_sources",
            )
            outs.append("%s_types" % name)
            jsouts.append("%s_tsdev" % name)

        _closure_js_library(
            name = "%s_js" % name,
            srcs = js_srcs + jsouts,
            deps = deps + [
                "//third_party/google/tsickle:tslib",
            ],
            **js_args
        )
        _closure_js_binary(
            name = "%s_jsbin" % name,
            deps = [":%s_js" % name],
            defs = compiler_args + (
                ["-D%s=%s" % i for i in (_RUNTIME_DEFINES.items() if _RUNTIME_DEFINES else [])]
            ),
            **kwargs
        )
        native.filegroup(
            name = "%s_module_src" % name,
            srcs = [
                package_json,
                "%s_jsbin" % name,
            ],
        )

    _pkg_files(
        name = "%s.tarfiles" % name,
        srcs = [":%s_module_src" % name],
    )
    _pkg_filegroup(
        name = "%s.tarfilegroup" % name,
        srcs = [":%s.tarfiles" % name],
        prefix = "node_modules/%s/" % (module or name),
    )
    _pkg_tar(
        name = "%s.tarball" % name,
        out = "%s.tar" % name,
        srcs = [":%s_module_src" % name],
        package_dir = "node_modules/%s/" % (module or name),
    )
    native.alias(
        name = name,
        actual = "%s.tarfilegroup" % name,
    )

def _js_runtime(
        name,
        main,
        entrypoint,
        deps,
        ts_deps = [],
        extra_production_args = [],
        esbuild_opt = None,
        extra_sources = [],
        **kwargs):
    """Single-use target macro which defines the main application entry target for the Elide JavaScript runtime."""

    config = {}
    config.update(_common_js_library_config)
    config.update(_base_js_runtime_config)
    config.update(kwargs)

    compiler_args = [] + _RUNTIME_JS_ARGS + config.get("extra_production_args", [])

    native.filegroup(
        name = "%s.typings" % name,
        srcs = [main] + ts_deps,
        output_group = "types",
    )

    _closure_js_binary(
        name = "%s.jsbin" % name,
        entry_points = [entrypoint],
        defs = compiler_args + (
            ["-D%s=%s" % i for i in (_RUNTIME_DEFINES.items() if _RUNTIME_DEFINES else [])]
        ),
        deps = deps + [main],
        **config
    )

    _esbuild_config(
        name = "%s.esbuildconfig" % name,
        config_file = "//elide/runtime/js/tools:esbuild.config.js",
    )
    _esbuild(
        name = "%s.jsopt" % name,
        srcs = [":%s.jsbin" % name],
        entry_point = "%s.jsbin.js" % name,
        format = "esm",
        output = "%s.bin.js" % name,
        sourcemap = "external",
        target = _JS_TARGET,
        config = ":%s.esbuildconfig" % name,
    )
    native.genrule(
        name = "%s.compressed.gen" % name,
        srcs = (extra_sources or []) + ["%s.bin.js" % name],
        outs = ["runtime.js.gz"],
        cmd = "gzip --force --best --to-stdout $(SRCS) > $(OUTS)",
    )
    native.genrule(
        name = "%s.compressed.sha256" % name,
        srcs = ["runtime.js.gz"],
        outs = ["runtime.js.gz.sha256"],
        cmd = "shasum -a 256 $(SRCS) | cut -d ' ' -f 1 > $(OUTS)",
    )
    native.filegroup(
        name = "%s.compressed" % name,
        srcs = [
            "runtime.js.gz",
            "runtime.js.gz.sha256",
            ":%s.typings" % name,
        ],
    )
    native.alias(
        name = name,
        actual = "%s.jsopt" % name,
    )

def _ts_runtime(
        name,
        main,
        ts_config,
        deps,
        closure_deps = [],
        closure_lib_kwargs = {},
        closure_bin_kwargs = {},
        extra_sources = [],
        extra_production_args = [],
        **kwargs):
    """Single-use target macro which defines the main application entry target for the Elide TypeScript runtime."""

    tsc = {}
    tsc.update(_ts_compiler_args)
    tsc.update(kwargs)

    _ts_library(
        name = "%s-tslib" % name,
        srcs = [main],
        deps = deps,
        module = "@elide/runtime/ts",
        nowrap = True,
        tsconfig = ts_config,
        devmode_target = "es2020",
        prodmode_target = "es2020",
        **tsc
    )
    native.filegroup(
        name = "%s-typings" % name,
        srcs = [":%s-tslib" % name],
        output_group = "types",
    )
    native.filegroup(
        name = "%s-devsrc" % name,
        srcs = [":%s-tslib" % name],
        output_group = "es5_sources",
    )
    native.filegroup(
        name = "%s-prodsrc" % name,
        srcs = [":%s-tslib" % name],
        output_group = "es6_sources",
    )
    _closure_js_library(
        name = "%s-js" % name,
        srcs = [":%s-devsrc" % name],
        deps = closure_deps,
        **closure_lib_kwargs
    )

    compiler_args = [] + _RUNTIME_JS_ARGS + extra_production_args

    _closure_js_binary(
        name = "%s.jsbin" % name,
        entry_points = ["elide.runtime.ts.entrypoint"],
        defs = compiler_args + (
            ["-D%s=%s" % i for i in (_RUNTIME_DEFINES.items() if _RUNTIME_DEFINES else [])]
        ),
        deps = [":%s-js" % name] + closure_deps,
        **closure_bin_kwargs
    )

    native.filegroup(
        name = "%s-files" % name,
        srcs = [
            ":%s-typings" % name,
            ":%s.jsbin" % name,
        ] + extra_sources,
    )
    _pkg_tar(
        name = "tsruntime.tarball",
        out = "runtime.ts.tar",
        srcs = [":%s-files" % name],
    )
    native.alias(
        name = name,
        actual = "tsruntime.tarball",
    )

def _runtime_dist(name, language, target, manifest, info = [], configs = [], modules = [], extra_sources = []):
    """ """

    outs = []

    native.filegroup(
        name = "distfiles",
        srcs = [
            "runtime.js.gz",
            "runtime.js.gz.sha256",
        ] + configs + extra_sources,
    )
    if len(modules) > 0:
        _pkg_tar(
            name = "%s.modules" % language,
            out = "%s.modules.tar.gz" % language,
            srcs = modules,
        )
        outs.append(":%s.modules" % language)
    _pkg_tar(
        name = "%s.tarball" % language,
        out = "%s.dist.tar" % language,
        srcs = [":distfiles"] + modules,
    )
    _jar_resources(
        name = "%s.runtime" % language,
        language = language,
        manifest = manifest,
        srcs = [":distfiles"],
    )
    native.filegroup(
        name = "distributions",
        srcs = [
            ":%s.tarball" % language,
            ":%s.runtime" % language,
        ] + info,
    )

    _pkg_tar(
        name = "dist-all",
        out = "%s.dist-all.tar.gz" % language,
        srcs = [":distributions"],
    )
    native.filegroup(
        name = "dist-all-outs",
        srcs = [
            ":dist-all",
        ] + outs,
    )

    native.alias(
        name = name,
        actual = ":dist-all-outs",
    )
    native.alias(
        name = language,
        actual = name,
    )

## Exports.
elide_test = _elide_test
js_library = _js_library
js_runtime = _js_runtime
js_module = _js_module
ts_library = _ts_library
ts_config = _ts_config
ts_runtime = _ts_runtime
runtime_dist = _runtime_dist
