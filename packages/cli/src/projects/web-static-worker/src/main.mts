/**
 * Sample script to demonstrate the use of TypeScript with Elide in a static
 * site context.
 */

function sayHello(name: string): string {
  return `Hello, ${name}, from TypeScript!`
}

document.addEventListener("DOMContentLoaded", () => {
  console.log(sayHello("World"))
})
