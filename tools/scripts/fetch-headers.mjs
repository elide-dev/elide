console.log("Response: ", Headers)

const empty = new Headers()
const fromMap = new Headers({ a: "b" })
const fromExisting = new Headers(fromMap)

console.log("Empty: ", empty)
console.log("From Map: ", fromMap)
console.log("From Existing: ", fromExisting)
