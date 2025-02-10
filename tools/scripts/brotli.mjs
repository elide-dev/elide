import { readFileSync, writeFileSync } from "node:fs"
import { join, normalize, resolve } from "node:path"
import { brotliCompressSync, brotliDecompressSync } from "node:zlib"

const file = "README.md"
const joined = process.argv[2] ? resolve(normalize(process.argv[2])) : join(process.cwd(), file)
const out = process.argv[3] ? resolve(normalize(process.argv[3])) : [joined.split(".")[0], "md.br"].join(".")

console.log(`Reading '${joined}'...`)

const content = readFileSync(joined, { encoding: "utf-8" })

console.log(`Compressing to '${out}' with Brotli...`)

const compressed = brotliCompressSync(content)

writeFileSync(out, compressed)
