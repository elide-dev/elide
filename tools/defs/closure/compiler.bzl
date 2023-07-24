"""Initialization code for Closure Compiler via Bazel."""

load(
    "@bazel_tools//tools/build_defs/repo:java.bzl",
    _java_import_external = "java_import_external",
)

version = "v20221102"
jar = "closure-compiler-%s.jar" % version

def setup_closure_compiler():
    """Setup a pinned version of the Closure Compiler."""

    _java_import_external(
        name = "com_google_javascript_closure_compiler",
        licenses = ["reciprocal"],
        jar_urls = [
            "https://repo1.maven.org/maven2/com/google/javascript/closure-compiler/%s/%s" % (version, jar),
        ],
        jar_sha256 = None,
        deps = [
            "@com_google_code_gson",
            "@com_google_guava",
            "@com_google_code_findbugs_jsr305",
            "@com_google_protobuf//:protobuf_java",
        ],
        extra_build_file_content = "\n".join([
            "java_binary(",
            "    name = \"main\",",
            "    main_class = \"com.google.javascript.jscomp.CommandLineRunner\",",
            "    output_licenses = [\"unencumbered\"],",
            "    runtime_deps = [",
            "        \":com_google_javascript_closure_compiler\",",
            "        \"@args4j\",",
            "    ],",
            ")",
            "",
            "genrule(",
            "    name = \"externs\",",
            "    srcs = [\"%s\"]," % jar,
            "    outs = [\"externs.zip\"],",
            "    tools = [\"@bazel_tools//tools/jdk:jar\"],",
            "    cmd = \"$(location @bazel_tools//tools/jdk:jar) -xf $(location :%s) externs.zip; mv externs.zip $@\"," % jar,
            ")",
            "",
        ]),
    )
