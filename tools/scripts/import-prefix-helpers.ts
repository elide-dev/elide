/// <reference path="../../packages/types/index.d.ts" />

import { Database } from "elide:sqlite"
import { ok } from "node:assert"

export function createDatabase(): Database {
  const db = new Database(":memory:")
  ok(db, "Database should be created")
  return db
}

export function testDatabase() {
  const db = createDatabase()

  db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT, value INTEGER);")
  db.exec("INSERT INTO test (name, value) VALUES ('foo', 42);")
  db.exec("INSERT INTO test (name, value) VALUES ('bar', 84);")

  const query = db.query("SELECT * FROM test;")
  const results = query.all()

  ok(results.length === 2, `Expected 2 results, got ${results.length}`)
  ok(results[0].name === "foo", `Expected name 'foo', got '${results[0].name}'`)
  ok(results[0].value === 42, `Expected value 42, got ${results[0].value}`)
  ok(results[1].name === "bar", `Expected name 'bar', got '${results[1].name}'`)
  ok(results[1].value === 84, `Expected value 84, got ${results[1].value}`)

  db.close()
  console.info("Database test passed")
}
