const process = this["process"] || {
  pid: -1,
  cwd: () => "",
  env: {},
  NODE_DEBUG: false,
  NODE_ENV: "production",
  noDeprecation: false,
  browser: false,
  version: 'v18.9.0'
};

globalThis.process = process;

const window = globalThis || this;
const global = globalThis || this;
const self = globalThis || this;

let gc = null;

const Elide = {
  process,
  window,
  global,
  self,
  context: {
    build: false,
    runtime: true
  }
};
globalThis.Elide = Elide;
