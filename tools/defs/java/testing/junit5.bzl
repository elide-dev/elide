##
# Copyright © 2022, The Elide Framework Authors. All rights reserved.
#
# The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
# are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
# this code in object or source form requires and implies consent and agreement to that license in principle and
# practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
# Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
# Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
# by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
# is strictly forbidden except in adherence with assigned license requirements.
##

"""External dependencies & java_junit5_test rule"""

load(
    "@bazel_tools//tools/build_defs/repo:jvm.bzl",
    "jvm_maven_import_external",
)
load(
    "@rules_java//java:defs.bzl",
    _java_test = "java_test",
)
load(
    "@io_bazel_rules_kotlin//kotlin:jvm.bzl",
    _kt_jvm_test = "kt_jvm_test",
)

JUNIT_JUPITER_GROUP_ID = "org.junit.jupiter"
JUNIT_JUPITER_ARTIFACT_ID_LIST = [
    "junit-jupiter-api",
    "junit-jupiter-engine",
    "junit-jupiter-params",
]

JUNIT_PLATFORM_GROUP_ID = "org.junit.platform"
JUNIT_PLATFORM_ARTIFACT_ID_LIST = [
    "junit-platform-commons",
    "junit-platform-console",
    "junit-platform-engine",
    "junit-platform-launcher",
    "junit-platform-suite-api",
]

JUNIT_EXTRA_DEPENDENCIES = [
    ("org.apiguardian", "apiguardian-api", "1.0.0"),
    ("org.opentest4j", "opentest4j", "1.1.1"),
]

TEST_RESOURCES = [
    "//javatests:application.yml",
    "//javatests:logback-test.xml",
]

def junit_jupiter_java_repositories(
        version = "5.8.2"):
    """Imports dependencies for JUnit Jupiter"""
    for artifact_id in JUNIT_JUPITER_ARTIFACT_ID_LIST:
        jvm_maven_import_external(
            name = _format_maven_jar_name(JUNIT_JUPITER_GROUP_ID, artifact_id),
            artifact = "%s:%s:%s" % (
                JUNIT_JUPITER_GROUP_ID,
                artifact_id,
                version,
            ),
            server_urls = ["https://maven.pkg.st"],
            licenses = ["notice"],  # EPL 2.0 License
        )

    for t in JUNIT_EXTRA_DEPENDENCIES:
        jvm_maven_import_external(
            name = _format_maven_jar_name(t[0], t[1]),
            artifact = "%s:%s:%s" % t,
            server_urls = ["https://maven.pkg.st"],
            licenses = ["notice"],  # EPL 2.0 License
        )

def junit_platform_java_repositories(
        version = "1.8.2"):
    """Imports dependencies for JUnit Platform"""
    for artifact_id in JUNIT_PLATFORM_ARTIFACT_ID_LIST:
        jvm_maven_import_external(
            name = _format_maven_jar_name(JUNIT_PLATFORM_GROUP_ID, artifact_id),
            artifact = "%s:%s:%s" % (
                JUNIT_PLATFORM_GROUP_ID,
                artifact_id,
                version,
            ),
            server_urls = ["https://maven.pkg.st"],
            licenses = ["notice"],  # EPL 2.0 License
        )

def junit5_repositories():
    junit_jupiter_java_repositories()
    junit_platform_java_repositories()

def _wire_junit5_test(
        rule,
        name,
        srcs,
        test_package,
        deps = [],
        runtime_deps = [],
        extra_filter_args = [],
        **kwargs):
    FILTER_KWARGS = [
        "main_class",
        "use_testrunner",
        "args",
    ] + extra_filter_args

    for arg in FILTER_KWARGS:
        if arg in kwargs.keys():
            kwargs.pop(arg)

    junit_console_args = []
    if test_package:
        junit_console_args += ["--select-package", test_package]
    else:
        fail("must specify 'test_package'")

    rule(
        name = name,
        srcs = srcs,
        main_class = "org.junit.platform.console.ConsoleLauncher",
        args = junit_console_args,
        deps = deps + [
            _format_maven_jar_dep_name(JUNIT_JUPITER_GROUP_ID, artifact_id)
            for artifact_id in JUNIT_JUPITER_ARTIFACT_ID_LIST
        ] + [
            _format_maven_jar_dep_name(JUNIT_PLATFORM_GROUP_ID, "junit-platform-suite-api"),
        ] + [
            _format_maven_jar_dep_name(t[0], t[1])
            for t in JUNIT_EXTRA_DEPENDENCIES
        ],
        runtime_deps = runtime_deps + [
            _format_maven_jar_dep_name(JUNIT_PLATFORM_GROUP_ID, artifact_id)
            for artifact_id in JUNIT_PLATFORM_ARTIFACT_ID_LIST
        ],
        **kwargs
    )

def java_junit5_test(
        name,
        srcs,
        test_package,
        deps = [],
        use_testrunner = False,
        classpath_resources = [],
        runtime_deps = [],
        **kwargs):
    _wire_junit5_test(
        _java_test,
        name = name,
        srcs = srcs,
        test_package = test_package,
        deps = deps,
        runtime_deps = runtime_deps,
        use_testrunner = use_testrunner,
        classpath_resources = classpath_resources + TEST_RESOURCES,
        **kwargs
    )

def kt_junit5_test(name, srcs, test_package, deps = [], runtime_deps = [], **kwargs):
    _wire_junit5_test(
        _kt_jvm_test,
        name = name,
        srcs = srcs,
        test_package = test_package,
        deps = deps,
        runtime_deps = runtime_deps,
        extra_filter_args = ["use_testrunner"],
        **kwargs
    )

def _format_maven_jar_name(group_id, artifact_id):
    return ("%s_%s" % (group_id, artifact_id)).replace(".", "_").replace("-", "_")

def _format_maven_jar_dep_name(group_id, artifact_id):
    return "@%s//jar" % _format_maven_jar_name(group_id, artifact_id)
