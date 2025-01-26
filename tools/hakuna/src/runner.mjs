import { join, resolve, normalize } from "node:path"

const args = process.argv.slice(2)

function buildImportFromCwd(relative) {
  return resolve(normalize(join(process.cwd(), relative)))
}

// grab all args after the string that contains `runner.mjs`
const benchmarks = args.slice(args.indexOf("./tools/hakuna/src/runner.mjs") + 1)
if (benchmarks) {
  // force-import all benchmarks, which registers them to the runner
  await Promise.all(benchmarks.map(bench => import(buildImportFromCwd(bench))))
}

console.info("Benchmarks loaded. Running...")

const { run } = await import("mitata")

await run({
  units: false, // print small units cheatsheet
  silent: false, // enable/disable stdout output
  avg: true, // enable/disable avg column (default: true)
  json: true, // enable/disable json output (default: false)
  colors: true, // enable/disable colors (default: true)
  min_max: true, // enable/disable min/max column (default: true)
  percentiles: true, // enable/disable percentiles column (default: true)
})
