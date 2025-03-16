import { join } from "node:path"

const pyfile = join(process.cwd(), "tools", "scripts", "say_hello_raw.py")
Polyglot.evalFile("python", pyfile)
const sayHello = Polyglot.import("say_hello")

// this line exists to show that this is typescript lol
const msg: () => string = () => `${sayHello()} + TypeScript!`
console.log(JSON.stringify({ x: msg() }))
