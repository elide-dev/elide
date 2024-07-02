import { ok } from "node:assert"
import fs from "node:fs"
import path from "node:path"

const joined = path.resolve(path.join(".", "README.md"))

console.info("Reading:", joined)

const contents = fs.readFileSync(joined, { encoding: "utf-8" })
ok(contents)
console.info("First line of contents", contents.split("\n")[0])

fs.readFile(joined, { encoding: "utf-8" }, (err, contents) => {
  if (err) {
    console.error(err)
    return
  }
  console.info("First line of contents (async)", contents.split("\n")[0])
})
