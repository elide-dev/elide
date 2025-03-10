import path from "node:path"

const cwd = process.cwd()
console.log("cwd: ", cwd)
const this_plus_dev = path.join(cwd, ".dev", "cool", "..")
console.log("dev: ", this_plus_dev)
const actually_just_dev = path.resolve(this_plus_dev)
console.log("path: ", actually_just_dev)
