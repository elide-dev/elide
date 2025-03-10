import { ok } from "node:assert"
import { readFileSync } from "node:fs"
import { join } from "node:path"

ok(true)

const joined = join(process.cwd(), ".", "README.md")

console.info("Reading:", joined)

const contents = readFileSync(joined, { encoding: "utf-8" })
ok(contents)
console.info("First line of contents", contents.split("\n")[0])
