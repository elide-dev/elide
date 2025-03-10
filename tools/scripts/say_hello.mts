Polyglot.evalFile("python", "/home/sam/workspace/elide/tools/scripts/say_hello.py")
const message = Polyglot.import("message")

// this line exists to show that this is typescript lol
const msg: (boolean) => string = leaving => `${message(leaving)()} + TypeScript!`
console.log(JSON.stringify({ x: msg(false) }))
console.log(JSON.stringify({ x: msg(true) }))
