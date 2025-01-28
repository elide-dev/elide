import { join, normalize, resolve } from "node:path"
import { run } from "mitata"

const allBenchmarks = ["basic", "sqlite"].map(name => {
  return resolve(normalize(join("tools", "bench", "js", `${name}.mjs`)))
})

async function importAllBenchmarks() {
  await importSpecifiedBenchmarks(allBenchmarks)
}

async function importSpecifiedBenchmarks(files) {
  for (const file of files) {
    await import(file)
  }
}

async function runBenchmarks(args) {
  await importAllBenchmarks()

  await run({
    units: false, // print small units cheatsheet
    silent: false, // enable/disable stdout output
    avg: true, // enable/disable avg column (default: true)
    json: false, // enable/disable json output (default: false)
    colors: true, // enable/disable colors (default: true)
    min_max: true, // enable/disable min/max column (default: true)
    percentiles: true, // enable/disable percentiles column (default: true)
  })
}

await runBenchmarks(process.argv)
