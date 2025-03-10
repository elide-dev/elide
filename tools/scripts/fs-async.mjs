import { ok } from "node:assert"
import { readFile } from "node:fs"
import { join } from "node:path"

ok(true)

const joined = join(process.cwd(), ".", "README.md")

console.info("Reading:", joined)

readFile(joined, { encoding: "utf-8" }, (err, contents) => {
  if (err) {
    console.error(err)
    return
  }
  console.info("First line of contents (async)", contents.split("\n")[0])
})
