const process = this["process"] || {
  pid: -1,
  cwd: () => "",
  env: {},
  NODE_DEBUG: false,
  NODE_ENV: "production",
  noDeprecation: false,
};

const window = globalThis || this;
const global = globalThis || this;
const self = globalThis || this;
let gc = null;
