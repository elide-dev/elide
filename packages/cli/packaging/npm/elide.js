'use strict';

const version = '1.0-v3-alpha3-b6';

const os = require('os');
const fs = require('fs');
const path = require('path');
const spawn = require('child_process').spawn;
const wget = require('node-wget');

function getNativeBinary() {
  const arch = {
    'arm64': 'aarch64',
    'x64': 'amd64',
  }[os.arch()];
  // Filter the platform based on the platforms that are build/included.
  const platform = {
    'darwin': 'darwin',
    'linux': 'linux',
  }[os.platform()];
  const extension = {
    'darwin': '',
    'linux': '',
  }[os.platform()];

  if (arch === undefined || platform === undefined) {
    console.error(`FATAL: Your platform/architecture combination ${
      os.platform()} - ${os.arch()} is not yet supported.
    You may need to compile Elide yourself, or use the JVM version.
    See instructions at https://github.com/elide-dev/v3.`);
    return Promise.resolve(1);
  }

  const binary =
    path.join(__dirname, `elide-${arch}-${platform}-${extension}`);
  return {binary, os, platform, arch};
}

function downloadBinary(target, os, arch, andThen) {
  const url = `https://dl.elide.dev/cli/v1/snapshot/${os}-${arch}/${version}/elide.gz`;
  console.log(`Downloading Elide (url: $url)...`);
  wget({
    url,
    dest: target
  }, () => {
    andThen();
  });
}

function spawnBinary(binary, args) {
  const ps = spawn(binary, args, { stdio: 'inherit' });

  function shutdown() {
    ps.kill("SIGTERM")
    process.exit();
  }

  process.on("SIGINT", shutdown);
  process.on("SIGTERM", shutdown);

  ps.on('close', e => process.exitCode = e);
}

function main(args) {
  const {binary, os, arch} = getNativeBinary();
  if (!fs.existsSync(binary)) {
    downloadBinary(binary, os, arch, () => {
      if (!fs.existsSync(binary)) {
        console.error(
          "Elide failed to download. Please use the one-line installer: " +
          "'curl -sSL --tlsv1.2 dl.elide.dev/cli/install.sh | bash'"
        );
      } else {
        console.log("Elide installed.");
        spawnBinary(binary, args);
      }
    });
  } else {
    spawnBinary(binary, args);
  }
}

if (require.main === module) {
  main(process.argv.slice(2));
}

module.exports = {
  getNativeBinary,
};
