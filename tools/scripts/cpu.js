const os = require("node:os")
const cpus = os.cpus()
console.log(`CPU info: ${JSON.stringify(cpus, null, 2)}`)
