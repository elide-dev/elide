/**
 * Sample script to demonstrate the use of plain JavaScript with Elide in a static
 * site context.
 */

function sayHello(name) {
  return `Hello, ${name}, from JavaScript!`
}

document.addEventListener("DOMContentLoaded", () => {
  console.log(sayHello("World"))
})
