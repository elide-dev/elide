import { bench, group } from "mitata"

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
