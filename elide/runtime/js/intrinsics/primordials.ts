/**
 * ## API: Base64
 *
 * Defines the API surface of Base64 encoder and decoder functions provided to the Elide JS VM. These functions are
 * available at the standard `atob` / `btoa` names within the global context, in addition to the `Base64` static class
 * functions `encode` and `decode`.
 */
export interface Base64 {
    /**
     * Encode the provided `input` string to Base64.
     *
     * @param input Input string to encode.
     * @return Encoded string.
     */
    encode(input: string): string;

    /**
     * Decode the provided `input` string from Base64.
     *
     * @param input Input string to decode.
     * @return Decoded string.
     */
    decode(input: string): string;
}

/**
 * # API: Console
 *
 * Defines the surface of the Console API for Elide. Elide's console intrinsics are standards-compliant with the WhatWG
 * Console API Specification, within reason considering the limitations of JavaScript on the server-side. See below for
 * an exhaustive list of differences with the standard.
 *
 * ### WhatWG Console Specification: Unsupported Features
 *
 * - Extended or non-spec methods for performance measurement are not available (`profile`, etc)
 * - Methods related to "grouping" log messages do nothing, since many server-side log systems are not capable of
 *   grouping messages after the fact.
 */
export interface IConsole {
    /**
     * ## Logging: `TRACE` (`trace`)
     *
     * Log a message to the console at the `TRACE` level.
     *
     * @param args Arguments of any type, which the developer wishes to emit to the console; any number of arguments may
     *   be passed, and each argument will be emitted to the same console message call.
     */
    trace(...args: any[]): void;

    /**
     * ## Logging: `DEBUG` (`log`)
     *
     * Log a message to the console at the `DEBUG` level.
     *
     * @param args Arguments of any type, which the developer wishes to emit to the console; any number of arguments may
     *   be passed, and each argument will be emitted to the same console message call.
     */
    log(...args: any[]): void;

    /**
     * ## Logging: `DEBUG` (`log`)
     *
     * Log a message to the console at the `DEBUG` level.
     *
     * @param args Arguments of any type, which the developer wishes to emit to the console; any number of arguments may
     *   be passed, and each argument will be emitted to the same console message call.
     */
    debug(...args: any[]): void;

    /**
     * ## Logging: `INFO` (`info`)
     *
     * Log a message to the console at the `INFO` level.
     *
     * @param args Arguments of any type, which the developer wishes to emit to the console; any number of arguments may
     *   be passed, and each argument will be emitted to the same console message call.
     */
    info(...args: any[]): void;

    /**
     * ## Logging: `WARN` (`warn`)
     *
     * Log a message to the console at the `WARN` level.
     *
     * @param args Arguments of any type, which the developer wishes to emit to the console; any number of arguments may
     *   be passed, and each argument will be emitted to the same console message call.
     */
    warn(...args: any[]): void;

    /**
     * ## Logging: `ERROR` (`error`)
     *
     * Log a message to the console at the `ERROR` level.
     *
     * @param args Arguments of any type, which the developer wishes to emit to the console; any number of arguments may
     *   be passed, and each argument will be emitted to the same console message call.
     */
    error(...args: any[]): void;
}
