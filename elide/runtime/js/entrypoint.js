// noinspection JSUnresolvedFunction

/*global goog*/

goog.module('elide.runtime.js.entry');

// Top-level Intrinsics.
goog.require('elide.runtime.js.bridge.jserror');
goog.require('elide.runtime.js.intrinsics.base64');
goog.require('elide.runtime.js.intrinsics.console');
goog.require('elide.runtime.js.intrinsics.err.ValueError');
goog.require('elide.runtime.js.intrinsics.url.URL');

/**
 * Type structure of a Node process object.
 *
 * @typedef {{
 *   cwd: (function(): string),
 *   NODE_DEBUG: boolean,
 *   noDeprecation: boolean,
 *   browser: boolean,
 *   pid: number,
 *   NODE_ENV: string,
 *   env: !Object<string, string>,
 *   version: string
 * }}
 */
let NodeProcess;

/**
 * Global Node.js-style `process` object.
 *
 * @type {!NodeProcess}
 */
const process = {
    'pid': -1,
    'cwd': () => "",
    'env': {},
    'NODE_DEBUG': false,
    'NODE_ENV': "production",
    'noDeprecation': false,
    'browser': false,
    'version': 'v18.9.0'
};

globalThis['process'] = process;
globalThis['window'] = undefined;
globalThis['gc'] = null;

/**
 * Global application object.
 *
 * @type {!Object<string, *>}
 */
const App = {};
globalThis['global'] = App;
globalThis['self'] = App;

/**
 * Global Elide object.
 *
 * @type {{
 *   process: !NodeProcess,
 *   context: {build: boolean, runtime: boolean},
 *   self: *
 * }}
 */
const Elide = {
    'process': process,
    'self': globalThis,
    'context': {
        'build': false,
        'runtime': true
    },
    'App': App,
};
globalThis['Elide'] = Elide;
