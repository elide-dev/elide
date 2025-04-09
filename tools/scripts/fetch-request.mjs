console.log("Request: ", Request)

const url = new URL("http://localhost:8080/")
const get = new Request(url)
console.log("GET: ", get)
