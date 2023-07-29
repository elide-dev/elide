
binpath=${1:-$(pwd)/packages/cli/build/native/nativeOptimizedCompile/elide};

echo "Establishing temp folder...";
workspace=$(mktemp -d);

cd "$workspace";
echo "Copying $binpath...";
cp -f "$binpath" ./elide.orig;
cp ./elide.orig ./elide;

## Compress with no upx
echo "Compressing original binary...";
xz -9kv elide;
mv elide.xz elide.orig.xz;

## Begin compression with upx (level 2)
upx -v -1 elide && xz -9kv elide && mv elide elide.2 && mv elide.xz elide.2.xz;

## Show sizes
du -h elide*;

echo "Done.";
exit 0;
