import { bench, group, run } from "mitata"

// deno
// import { ... } from 'npm:mitata';

// d8/jsc
// import { ... } from '<path to mitata>/src/cli.mjs';

bench("noop", () => {})
bench("noop2", () => {})

group("group", () => {
  // baseline("baseline", () => {})
  bench("Date.now()", () => Date.now())
  bench("performance.now()", () => performance.now())
})

group("new Array", () => {
  bench("new Array(0)", () => new Array(0))
  bench("new Array(1024)", () => new Array(1024))
})

await run({
  units: false, // print small units cheatsheet
  silent: false, // enable/disable stdout output
  avg: true, // enable/disable avg column (default: true)
  json: false, // enable/disable json output (default: false)
  colors: true, // enable/disable colors (default: true)
  min_max: true, // enable/disable min/max column (default: true)
  percentiles: true, // enable/disable percentiles column (default: true)
})
