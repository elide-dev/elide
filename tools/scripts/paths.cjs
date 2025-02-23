const { join, resolve } = require("node:path")

const cwd = process.cwd()
const this_plus_dev = join(cwd, ".dev", "cool", "..")
const actually_just_dev = resolve(this_plus_dev)
console.log("path: ", actually_just_dev)
