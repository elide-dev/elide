
// We don't need JavaScript for document.write but for the sake having a richer
// example, here is a JavaScript module that uses our Java code.

goog.module('j2cl.samples.hello');

// Here we use goog.require to import the Java HelloWorld class to this module.
const HelloWorld = goog.require('elide.runtime.js.HelloWorld');

/**
 * Says hello to console!
 *
 * @return {void}
 */
function sayHello() {
    document.body.innerText = `${HelloWorld.getHelloWorld()} and JS!`;
}

// Export our method so it could be used outside of the module.
exports = {sayHello};
