/// <reference path="../../packages/types/index.d.ts" />

import { testDatabase } from "./import-prefix-helpers.ts"

console.info("Testing multi-file imports with elide:* modules...")
testDatabase()
console.info("All tests passed!")
