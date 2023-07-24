import { ValueError } from "../intrinsics/err";
import { installGlobal } from "../intrinsics/base";

/**
 * # JS: Error Info
 *
 * Interface for information about an error which is to be surfaced in the JavaScript guest context. Errors of this kind
 * carry a `message`, a `code`, and a potential `cause`, which is a recursive reference to another error payload.
 */
type ErrorInfo = {
    /** Error message string. */
    message?: string;

    /** Error code or ID. */
    code?: string;

    /** Cause of this error. */
    cause?: ErrorInfo;
};

/**
 * # JS: Error Options
 *
 * Interface for options which affect creating a bridged JavaScript error. The error can be created and thrown in the
 * same step, or throwing the error can be deferred.
 */
type ErrorOptions = {
    /** Whether to immediately throw the error created for the provided `info`. */
    immediate?: boolean;
};

/**
 * # JS: Error Constructor.
 *
 * Expected interface for JavaScript errors' constructors. This is included inline to explain to the Closure Compiler
 * how error constructors are expected to behave.
 */
interface JsErrorConstructor<E extends Error> {
    new(message?: string): E;
    (message?: string): E;
    readonly prototype: E;
}

/**
 * # JS: Intrinsic Error Bridge
 *
 * Describes the interface of a JavaScript error bridging intrinsic, which is responsible for surfacing JavaScript
 * errors which occur at a deeper level in the VM.
 *
 * When a Java-style exception is raised during VM processing, the outer VM host can reach in and use this bridge to
 * structure the error in a familiar way for JavaScript guest code. The exception is wrapped with contextual information
 * and re-thrown.
 */
export interface IntrinsicErrorBridge {
    /**
     * Create a `TypeError` with the provided `errInfo`.
     *
     * @param errInfo Error info to use to create the type error.
     * @param options Options which apply to throwing this error.
     * @return Error which should be thrown or consumed.
     * @export
     * @public
     */
    typeError(errInfo: string | ErrorInfo, options?: ErrorOptions): TypeError;

    /**
     * Create a `ValueError` with the provided `errInfo`.
     *
     * @param errInfo Error info to use to create the value error.
     * @param options Options which apply to throwing this error.
     * @return Error which should be thrown or consumed.
     * @export
     * @public
     */
    valueError(errInfo: string | ErrorInfo, options?: ErrorOptions): ValueError;
}

/**
 * Create an error from the provided `info`.
 *
 * @template E Error type.
 * @param ctor Constructor for the error type.
 * @param err Error info.
 * @param options Options which apply to throwing this error.
 * @return Error instance.
 * @suppress {checkTypes,reportUnknownTypes}
 */
function errFromInfo<E extends Error>(ctor: JsErrorConstructor<E>, err: string | ErrorInfo, options?: ErrorOptions): E {
    let msg;
    if (typeof err === "string") {
        msg = err;
    } else {
        msg = err.message;
    }
    const errObj: E = new ctor(msg);
    if (options?.immediate == false) {
        return errObj;
    } else {
        throw errObj;
    }
}

// Main implementation of the `IntrinsicErrorBridge` interface.
class IntrinsicErrorBridgeImpl implements IntrinsicErrorBridge {
    /** @inheritDoc */
    typeError(errInfo: string | ErrorInfo, options?: ErrorOptions): TypeError {
        return errFromInfo(TypeError as unknown as JsErrorConstructor<TypeError>, errInfo, options);
    }

    /** @inheritDoc */
    valueError(errInfo: string | ErrorInfo, options?: ErrorOptions): ValueError {
        let msg;
        if (typeof errInfo === "string") {
            msg = errInfo;
        } else {
            msg = errInfo.message;
        }
        const exc = new ValueError(msg);
        if (options?.immediate === false) {
            return exc;
        } else {
            throw exc;
        }
    }
}

/**
 * Main error bridge singleton.
 *
 * @see IntrinsicErrorBridge for the structure of this singleton.
 */
export const errBridge: IntrinsicErrorBridge = new IntrinsicErrorBridgeImpl();

// Install as global.
installGlobal("__errBridge", errBridge);
