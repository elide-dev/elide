import { ok } from "node:assert"
import fs from "node:fs/promises"
import path from "node:path"

const joined = path.resolve(path.join(process.cwd(), "README.md"))

console.info("Reading:", joined)

fs.readFile(joined, { encoding: "utf-8" }).then(
  contents => {
    console.info(`we got type: (${typeof contents}) ${contents}`)
    console.info("First line of contents", contents.split("\n")[0])
    ok(contents)
  },
  err => {
    console.error(err)
    throw err
  },
)
