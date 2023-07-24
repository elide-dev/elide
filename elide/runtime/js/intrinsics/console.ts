// noinspection JSUnusedGlobalSymbols

import {globalContext, installGlobal} from "./base";
import type {IConsole} from "./primordials";

/** Enumerates available log levels. */
export enum LogLevel {
    /** Debug-level logging: informs the console about expected program states. */
    DEBUG = 'debug',

    /** Info-level logging: informs the console about notable expected program states. */
    INFO = 'info',

    /** Warn-level logging: notifies the developer of a sub-optimal program state. */
    WARN = 'warn',

    /** Error-level logging: emits a message about an unexpected and invalid program state. */
    ERROR = 'error',
}

// Throw an `Error` describing an unsupported method.
function throwNotSupported(): Error {
    return new Error('Method not supported');
}

/**
 * Abstract console.
 *
 * Provides base implementation code for {@link IConsole} implementations. In particular, rollup and formatting of
 * arguments is implemented here.
 *
 * ### WhatWG Console Specification: Unsupported Features
 *
 * - Extended or non-spec methods for performance measurement are not available (`profile`, etc)
 * - Methods related to "grouping" log messages do nothing, since many server-side log systems are not capable of
 *   grouping messages after the fact.
 */
abstract class AbstractConsole implements IConsole {
    /**
     * Deliver a log message to the console backend.
     *
     * During this process, the provided `args` may be converted to string representations.
     *
     * @param level Log level of the message.
     * @param args Arguments (portions) of the message.
     */
    abstract deliverLog(level: LogLevel, args: any[]): void;

    /** @inheritDoc */
    trace(...args: any[]): void {
        this.deliverLog(LogLevel.DEBUG, args);
    }

    /** @inheritDoc */
    log(...args: any[]): void {
        this.deliverLog(LogLevel.DEBUG, args);
    }

    /** @inheritDoc */
    debug(...args: any[]): void {
        this.deliverLog(LogLevel.DEBUG, args);
    }

    /** @inheritDoc */
    info(...args: any[]): void {
        this.deliverLog(LogLevel.INFO, args);
    }

    /** @inheritDoc */
    warn(...args: any[]): void {
        this.deliverLog(LogLevel.WARN, args);
    }

    /** @inheritDoc */
    error(...args: any[]): void {
        this.deliverLog(LogLevel.ERROR, args);
    }

    /**
     * ## Console: `assert`
     *
     * Assert the provided `condition`, failing with `data` message portions if the condition is `false`.
     *
     * From MDN:
     * "The console.assert() method writes an error message to the console if the assertion is false. If the assertion
     * is true, nothing happens."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/assert
     *
     * @param condition Condition to assert.
     * @param data Arguments of any type, which the developer wishes to emit to the console; any number of arguments may
     *   be passed, and each argument will be emitted to the same console message call.
     */
    assert(condition?: boolean, ...data: any[]): void;

    /**
     * ## Console: `assert`
     *
     * Assert the provided `value` is truthy, failing with `message` if the value is `false`, and applying the provided
     * `optionalParams` to format the message components.
     *
     * From MDN:
     * "The console.assert() method writes an error message to the console if the assertion is false. If the assertion
     * is true, nothing happens."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/assert
     *
     * @param value Condition to assert.
     * @param message Message to display if the condition is `false`.
     * @param optionalParams Format parameters for the provided `message`.
     */
    assert(value?: unknown, message?: unknown, ...optionalParams: unknown[]): void {
        throw new Error("Method not implemented.");
    }

    /**
     * ## Console: `clear`
     *
     * Clear the terminal/console screen, where supported. Server-side, this operation is a no-op.
     *
     * From MDN:
     * "The console.clear() method clears the console if the console allows it. A graphical console, like those running
     * on browsers, will allow it; a console displaying on the terminal, like the one running on Node, will not support
     * it, and will have no effect (and no error)."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/clear
     */
    clear(): void {
        // no-op
    }

    /**
     * ## Console: `count`
     *
     * Apply an increment to the counter represented by `label`.
     *
     * From MDN:
     * "The console.count() method logs the number of times that this particular call to count() has been called."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/count
     *
     * @param label Label for the counter to increment.
     */
    count(label?: unknown): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `countReset`
     *
     * Reset the counter represented by `label`.
     *
     * From MDN:
     * "The console.countReset() method resets counter used with console.count()."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/countReset
     *
     * @param label Label for the counter to reset.
     */
    countReset(label?: unknown): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `dir`
     *
     * Where supported, display an interactive list of the properties of the provided `object`. Server-side, this method
     * may simply print a string representation of the object.
     *
     * From MDN:
     * "The method console.dir() displays an interactive list of the properties of the specified JavaScript object. The
     * output is presented as a hierarchical listing with disclosure triangles that let you see the contents of child
     * objects. In other words, console.dir() is the way to see all the properties of a specified JavaScript object in
     * console by which the developer can easily get the properties of the object."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/dir
     *
     * @param obj The JavaScript object to print a directory of.
     * @param options Options for the output.
     */
    dir(obj?: any | unknown, options?: any): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `dirxml`
     *
     * Where supported, display an interactive tree of the properties of the provided `object`. Server-side, this method
     * may simply print a string representation of the object.
     *
     * From MDN:
     * "The console.dirxml() method displays an interactive tree of the descendant elements of the specified XML/HTML
     * element. If it is not possible to display as an element the JavaScript Object view is shown instead. The output
     * is presented as a hierarchical listing of expandable nodes that let you see the contents of child nodes."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/dirxml
     *
     * @param data Object to print a directory of.
     */
    dirxml(...data: any[]): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `group`
     *
     * Create a new inline group within the output terminal, causing subsequent log messages to be enclosed within the
     * declared group. Server-side, this method may be a no-op or may emit a regular log message.
     *
     * From MDN:
     * "The console.group() method creates a new inline group in the Web console log, causing any subsequent console
     * messages to be indented by an additional level, until console.groupEnd() is called."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/group
     *
     * @param label Label to display for the group.
     */
    group(...label: any[]): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `groupCollapsed`
     *
     * Create a new inline group within the output terminal, causing subsequent log messages to be enclosed within the
     * declared group. The group should display as collapsed by default. Server-side, this method may be a no-op or may
     * emit a regular log message.
     *
     * From MDN:
     * "The console.groupCollapsed() method creates a new inline group in the Web Console. Unlike console.group(),
     * however, the new group is created collapsed. The user will need to use the disclosure button next to it to expand
     * it, revealing the entries created in the group."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/groupCollapsed
     *
     * @param label Label to display for the group.
     */
    groupCollapsed(...label: any[]): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `groupEnd`
     *
     * Finish the active inline log group, if active (otherwise, this method is a no-op). Server-side, this method may
     * be a no-op or may emit a regular log message.
     *
     * From MDN:
     * "The console.groupEnd() method exits the current inline group in the Web console. See Using groups in the console
     * in the console documentation for details and examples."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/groupEnd
     */
    groupEnd(): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `table`
     *
     * Emit a depiction of tabular data to the console, applying the provided `properties` to configure columns.
     *
     * From MDN:
     * "The console.table() method displays tabular data as a table. This function takes one mandatory argument `data`,
     * which must be an array or an object, and one additional optional parameter columns. It logs data as a table. Each
     * element in the array (or enumerable property if data is an object) will be a row in the table."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/table
     *
     * @param tabularData Tabular data to display; must be an array of rows, or an object.
     * @param properties Properties to consider as columns for the table.
     */
    table(tabularData?: unknown, properties?: unknown): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `time`
     *
     * Starts a named timer for an arbitrary operation.
     *
     * From MDN:
     * "The console.time() method starts a timer you can use to track how long an operation takes. You give each timer a
     * unique name, and may have up to 10,000 timers running on a given page. When you call console.timeEnd() with the
     * same name, the browser will output the time, in milliseconds, that elapsed since the timer was started."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/time
     *
     * @param label Label for the timer.
     */
    time(label?: string | unknown): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `timeEnd`
     *
     * Concludes a named timer for an arbitrary operation, which was previously started with `time`. When this method is
     * called, the timer result is typically logged.
     *
     * From MDN:
     * "The console.timeEnd() stops a timer that was previously started by calling console.time()."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/timeEnd
     *
     * @param label Label for the timer.
     */
    timeEnd(label?: string | unknown): void {
        throw throwNotSupported();
    }

    /**
     * ## Console: `timeLog`
     *
     * Log the time elapsed since the named timer was started, via `time`. The timer is not reset or otherwise modified
     * when using this method.
     *
     * From MDN:
     * "The console.timeLog() method logs the current value of a timer that was previously started by calling
     * console.time()."
     *
     * See also:
     * https://developer.mozilla.org/en-US/docs/Web/API/console/timeLog
     *
     * @param label Label for the timer.
     */
    timeLog(label: string | unknown): void {
        throw throwNotSupported();
    }
}

/** JS bridge to the console intrinsics. */
export class ConsoleBridge extends AbstractConsole {
    // Acquire the intrinsic console so that we can proxy to it.
    private acquireIntrinsic(): IConsole {
        return globalContext["Console"] as IConsole;
    }

    /** @inheritDoc */
    deliverLog(level: string, args: any[]) {
        const intrinsic = this.acquireIntrinsic();
        switch (level) {
            case 'trace': intrinsic.log(args); break;
            case 'debug': intrinsic.log(args); break;
            case 'info': intrinsic.info(args); break;
            case 'warn': intrinsic.warn(args); break;
            case 'error': intrinsic.error(args); break;
        }
    }
}

/** Main `console` global. */
export const console: IConsole = new ConsoleBridge();

declare global {
    // @ts-ignore
    export const console: IConsole;
}

installGlobal('console', console);
