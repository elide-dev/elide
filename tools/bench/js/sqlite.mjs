import { bench, group } from "mitata"

let cachedSqlite = null;

async function getSqlite() {
    let Database;
    if (cachedSqlite) {
        Database = cachedSqlite.Database;
    } else {
        if ('Bun' in globalThis) {
            const sqlite = require("bun:sqlite")
            cachedSqlite = sqlite
            Database = sqlite.Database
        } else {
            const sqlite = require("elide:sqlite")
            cachedSqlite = sqlite
            Database = sqlite.Database
        }
    }
    return {
        Database
    }
}

async function prepareDbThenQuery() {
    const { Database } = await getSqlite()
    const db = new Database()
    db.exec("CREATE TABLE cats (id INTEGER PRIMARY KEY, name TEXT)")
    db.exec("INSERT INTO cats (name) VALUES ('Bebe')")
    db.exec("INSERT INTO cats (name) VALUES ('Little Man')")
    return db.query("SELECT * FROM cats ORDER BY name;").all()
}

group("sqlite", () => {
  bench("prepareDbThenQuery", async () => await prepareDbThenQuery())
})

console.info("Priming SQLite...");
await prepareDbThenQuery();
