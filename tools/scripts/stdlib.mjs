import { equal } from "node:assert"
import path from "node:path"

const joined = path.join("some/path", "cool")

equal("some/path/cool", joined)
console.info("Passed", joined)
