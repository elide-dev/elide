console.log("Fetch: ", Fetch)
console.log("Headers: ", Headers)
console.log("Request: ", Request)
console.log("Response: ", Response)
console.log("fetch: ", fetch)

console.log("checkpoint pre")
const resp = await fetch("https://httpbin.org/status/200")
console.log("200: ", resp)

const goog = await fetch("https://google.com/")
console.log("google: ", goog)
