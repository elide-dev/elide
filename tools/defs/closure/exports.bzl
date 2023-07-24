"""Defines the export surface for code used from Closure and the Closure Rules for Bazel."""

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    _closure_js_library = "closure_js_library",
    _closure_js_binary = "closure_js_binary",
    _closure_js_proto_library = "closure_js_proto_library",
    _closure_js_template_library = "closure_js_template_library",
)

closure_js_library = _closure_js_library
closure_js_binary = _closure_js_binary
closure_js_proto_library = _closure_js_proto_library
closure_js_template_library = _closure_js_template_library
