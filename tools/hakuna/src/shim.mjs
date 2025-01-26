import { spawn } from "node:child_process"
import { existsSync } from "node:fs"
import { dirname, join } from "node:path"

const binName = "hakuna"
const thisFile = import.meta.filename
const potentialBin = join(dirname(dirname(thisFile)), "dist", binName)
const entryScript = join(dirname(thisFile), "entry.mjs")

if (existsSync(potentialBin)) {
  spawn(potentialBin, [entryScript, ...process.argv.slice(2)], { stdio: "inherit" })
} else {
  await import(entryScript)
}
