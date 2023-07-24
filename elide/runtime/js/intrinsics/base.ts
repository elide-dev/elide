/** Base interface for the VM global context. */
export interface VMGlobalContext extends Record<string, unknown> {
    [key: string]: any;
}

/** VM global context. */
export const globalContext: VMGlobalContext = globalThis as VMGlobalContext;

declare global {
    export const goog: any;
}

/**
 * Install a global value into the JavaScript VM.
 *
 * @param name Name to install the global value at.
 * @param value Value to install.
 * @suppress {reportUnknownTypes}
 */
export function installGlobal<T extends any>(name: string, value: T) {
    goog.exportSymbol(name, value);
    globalContext[name] = value;
    return value;
}
