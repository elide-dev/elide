"""Defines the export surface for code used from the standard packaging rules."""

load(
    "@rules_pkg//pkg:mappings.bzl",
    _pkg_attributes = "pkg_attributes",
    _pkg_filegroup = "pkg_filegroup",
    _pkg_files = "pkg_files",
)
load(
    "@rules_pkg//pkg:tar.bzl",
    _pkg_tar = "pkg_tar",
)
load(
    "@rules_pkg//pkg:zip.bzl",
    _pkg_zip = "pkg_zip",
)

## Exports.
pkg_tar = _pkg_tar
pkg_zip = _pkg_zip
pkg_filegroup = _pkg_filegroup
pkg_files = _pkg_files
pkg_attributes = _pkg_attributes
