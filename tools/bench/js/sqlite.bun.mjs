import { Database } from "bun:sqlite";

const db = new Database();
db.exec("CREATE TABLE cats (id INTEGER PRIMARY KEY, name TEXT)");
db.exec("INSERT INTO cats (name) VALUES ('Bebe')");
db.exec("INSERT INTO cats (name) VALUES ('Little Man')");
const cats = db.query("SELECT * FROM cats ORDER BY name;").all();
console.info("cats:", cats);

