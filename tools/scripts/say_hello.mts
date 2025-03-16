import { say_hello as sayHello } from "./say_hello.py"

// this line exists to show that this is typescript lol
const msg: () => string = () => `${sayHello()} + TypeScript!`
console.log(JSON.stringify({ x: msg() }))
