/// <reference path="../../packages/types/index.d.ts" />
const { Database } = require("elide:sqlite")

const db: Database = new Database()
db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
db.exec("INSERT INTO test (name) VALUES ('foo');")
const query = db.query("SELECT * FROM test LIMIT 1;")
const result = query.get()
const name = result.name
console.info(`Name: "${name}"`)
console.info("Done.")
