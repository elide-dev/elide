#!/usr/bin/env bash

set -euo pipefail

function publishLocal() {
  local artifact=$1
  local jarFile=$2

  echo "Publishing \"$artifact\" to local repository"
  ./mvnw \
    --settings $PWD/third_party/oracle/settings.xml \
    deploy:deploy-file \
    gpg:sign-and-deploy-file \
    -DpomFile=$PWD/third_party/oracle/${artifact}.pom \
    -Dfile=$PWD/third_party/oracle/${jarFile} \
    -DrepositoryId=local -Durl=file://$PWD/build/m2
}

for file in third_party/oracle/*.pom; do
  baseName=$(basename "$file" .pom)
  publishLocal "$baseName" "$baseName.jar"
  echo "Published $baseName to local repository".
done

echo "Tree of build/m2:"
tree -L 6 build/m2/org/graalvm

echo "Third-party Oracle libs ready."
