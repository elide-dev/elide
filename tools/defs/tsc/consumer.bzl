"""Rule that extracts the development-mode outputs from a TypeScript target."""

load(
    "@build_bazel_rules_nodejs//:providers.bzl",
    "JSEcmaScriptModuleInfo",
    "JSNamedModuleInfo",
)

def _devmode_consumer(ctx):
    sources_depsets = []
    for dep in ctx.attr.deps:
        if JSNamedModuleInfo in dep:
            sources_depsets.append(dep[JSNamedModuleInfo].sources)
    sources = depset(transitive = sources_depsets)

    return [DefaultInfo(
        files = sources,
        runfiles = ctx.runfiles(transitive_files = sources),
    )]

def _es6_consumer(ctx):
    sources_depsets = []
    for dep in ctx.attr.deps:
        if JSEcmaScriptModuleInfo in dep:
            sources_depsets.append(dep[JSEcmaScriptModuleInfo].sources)
    sources = depset(transitive = sources_depsets)

    return [DefaultInfo(
        files = sources,
        runfiles = ctx.runfiles(transitive_files = sources),
    )]

devmode_consumer = rule(
    implementation = _devmode_consumer,
    attrs = {
        "deps": attr.label_list(),
    },
)

es6_consumer = rule(
    implementation = _es6_consumer,
    attrs = {
        "deps": attr.label_list(),
    },
)
