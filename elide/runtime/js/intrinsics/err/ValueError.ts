import { installGlobal } from "../base";

/**
 * # JS: Value Error
 *
 * TBD.
 */
export class ValueError extends Error {
    protected __valueError__: boolean = true;

    /**
     * Error message for this error.
     *
     * @export
     */
    override readonly message: string;

    constructor(message?: string) {
        super(message);
        this.message = message || '`ValueError` was thrown';
    }

    /** @suppress {reportUnknownTypes} */
    static [Symbol.hasInstance](instance: any | ValueError): boolean {
        return (instance && instance.__valueError__ === true) || false;
    }
}

installGlobal("ValueError", ValueError);
