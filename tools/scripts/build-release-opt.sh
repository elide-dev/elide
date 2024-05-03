#!/bin/bash

version=$(cat ./.release)
platform=$(uname -s | tr '[:upper:]' '[:lower:]')
arch=$(uname -m)
variant="opt"

archive_prefix="elide"

if [ "$arch" = "arm64" ]; then
  arch="aarch64"
fi
platform="$platform-$arch"

# echo "platform: $platform"
# echo "variant: $variant"
# echo "version: $version"
# echo "arch: $arch"

if [ "$platform" = "" ]; then exit 2; fi
if [ "$variant" = "" ]; then exit 3; fi
if [ "$version" = "" ]; then exit 4; fi
if [ "$arch" = "" ]; then exit 5; fi

root=$(pwd)
echo "- Building release root (variant: $variant / platform: $platform)..."
pushd packages/cli/build/native/nativeOptimizedCompile \
  && mkdir -p "$archive_prefix-$version-$platform/" \
  && cp -fr elide ./*.{so,dylib,dll} $root/packages/cli/packaging/content/* resources "$archive_prefix-$version-$platform/"

echo "- Building tar package (variant: $variant / platform: $platform)..."
tar -cf "$archive_prefix-$version-$platform.tar" "$archive_prefix-$version-$platform/"

echo "- Building zip package (variant: $variant / platform: $platform)..."
zip -v -9 -r "$archive_prefix-$version-$platform.zip" "$archive_prefix-$version-$platform/"

echo "- Building tgz package (variant: $variant / platform: $platform)..."
gzip -v --best -k "$archive_prefix-$version-$platform.tar"
mv "$archive_prefix-$version-$platform.tar.gz" "$archive_prefix-$version-$platform.tgz"

echo "- Building txz package (variant: $variant / platform: $platform)..."
xz -v --best -k "$archive_prefix-$version-$platform.tar"
mv "$archive_prefix-$version-$platform.tar.xz" "$archive_prefix-$version-$platform.txz"

echo "- Stamping releases (variant: $variant / platform: $platform)..."

if [ "$(uname -o)" = "Darwin" ]; then
  SHA256SUM="gsha256sum"
  SHA512SUM="gsha512sum"
else
  SHA256SUM="sha256sum"
  SHA512SUM="sha512sum"
fi

for archive in ./elide*.{tgz,txz,zip}; do
  echo "- Archive \"$archive\"..."
  echo "-   SHA256..."
  $SHA256SUM "$archive" > "$archive.sha256"
  echo "-   SHA512..."
  $SHA512SUM "$archive" > "$archive.sha512"
  echo "-   GPG2..."
  gpg2 --detach-sign --batch --yes --armor "$archive"
  echo "-   Sigstore..."
  cosign sign-blob "$archive" --output-signature="$archive.sig" --output-certificate="$archive.pem" --tlog-upload=true -y
  echo ""
done

popd
