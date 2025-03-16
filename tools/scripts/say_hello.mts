import mod from "./say_hello.py"

// this line exists to show that this is typescript lol
const msg: () => string = () => `${mod.say_hello()} + TypeScript!`
console.log(JSON.stringify({ x: msg() }))
