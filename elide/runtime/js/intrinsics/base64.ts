import {globalContext, installGlobal} from "./base";
import type {Base64} from "./primordials";

/**
 * @return Intrinsic Base64 bridge.
 */
function resolveIntrinsic(): Base64 {
    return globalContext['Base64'] as Base64;
}

/**
 * Standard DOM `btoa` function which encodes a string to Base64.
 *
 * @param input Input string to encode.
 */
function base64Encode(input: string): string {
    return resolveIntrinsic().encode(input);
}

/**
 * Standard DOM `atob` function which decodes a string from base64.
 *
 * @param input Input string to decode.
 */
function base64Decode(input: string): string {
    return resolveIntrinsic().decode(input);
}

declare global {
    /**
     * Standard DOM `btoa` function which encodes a string to Base64.
     *
     * @param input Input string to encode.
     * @return Encoded string.
     */
    // @ts-ignore
    export const btoa: (input: string) => string;

    /**
     * Standard DOM `atob` function which decodes a string from base64.
     *
     * @param input Input string to decode.
     * @return Decoded string.
     */
    // @ts-ignore
    export const atob: (input: string) => string;
}

installGlobal('btoa', base64Encode);
installGlobal('atob', base64Decode);
