import { join, resolve } from "node:path"

const cwd = process.cwd()
console.log("cwd: ", cwd)
const this_plus_dev = join(cwd, ".dev", "cool", "..")
console.log("dev: ", this_plus_dev)
const actually_just_dev = resolve(this_plus_dev)
console.log("path: ", actually_just_dev)
