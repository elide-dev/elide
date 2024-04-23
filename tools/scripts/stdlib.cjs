
const { equal } = require("node:assert")
const path = require("node:path")
const joined = path.join("some/path", "cool")

equal("some/path/cool", joined)
console.info("Passed", joined)

