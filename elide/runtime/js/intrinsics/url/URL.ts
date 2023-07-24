// noinspection JSUnusedGlobalSymbols

import type {Blob} from "../blob/Blob";
// import type {IURLSearchParams} from "./IURLSearchParams";
// import type {URLSearchParams} from "./URLSearchParams";
import type {IURL, URLInputs} from "./IURL";

/** Main URL constructor instance. */
declare global {
    /** Intrinsic URL implementation. */
        // @ts-ignore
    const URL: {
        prototype: IURL;

        /**
         * Constructor: Absolute URL with a base URL.
         *
         * @param url URL string to parse, potentially as a relative URL.
         */
        new(url: URLInputs): IURL;

        /**
         * Constructor: Potentially-relative URL with a base URL.
         *
         * @param url URL string to parse, potentially as a relative URL.
         * @param base Base URL to use if `url` is relative.
         */
        new(url: URLInputs, base?: URLInputs): IURL;

        /**
         * Create a new `URL` object which references the provided [File] or [Blob] object.
         *
         * This method is not supported server-side.
         *
         * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/createObjectURL):
         * "The URL.createObjectURL() static method creates a string containing a URL representing the object given
         * in the parameter. The URL lifetime is tied to the document in the window on which it was created. The new
         * object URL represents the specified File object or Blob object. To release an object URL, call
         * revokeObjectURL()."
         *
         * @see createObjectURL to create a URL from a blob.
         * @see revokeObjectURL to revoke a created object URL.
         * @param obj `Blob`, or `File`, or `MediaSource` to create a URL for.
         * @return URL reference for the provided resource.
         */
        createObjectURL(obj: Blob): string;

        /**
         * Revoke a previously-issued temporary URL reference to a [File] or [Blob] object.
         *
         * This method is not supported server-side.
         *
         * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/revokeObjectURL):
         * "The `URL.revokeObjectURL()` static method releases an existing object `URL` which was previously created
         * by calling `URL.createObjectURL()`. Call this method when you've finished using an object `URL` to let
         * the browser know not to keep the reference to the file any longer."
         *
         * @see createObjectURL to create a URL from a file or blob.
         * @param url URL which was previously created via [createObjectURL], which should be revoked.
         */
        revokeObjectURL(url: string): void;
    };
}

/**
 * TBD.
 */
export interface ImmutableURL extends IURL {
    /** @inheritDoc */
    readonly hash: string;

    /** @inheritDoc */
    readonly host: string;

    /** @inheritDoc */
    readonly hostname: string;

    /** @inheritDoc */
    readonly href: string;

    /** @inheritDoc */
    readonly password: string;

    /** @inheritDoc */
    readonly pathname: string;

    /** @inheritDoc */
    readonly port: string;

    /** @inheritDoc */
    readonly protocol: string;

    /** @inheritDoc */
    readonly search: string;

    /** @inheritDoc */
    readonly origin: string;

    /** @inheritDoc */
    // readonly searchParams: IURLSearchParams;

    /** @inheritDoc */
    readonly username: string;
}

// Intrinsic-only API surface.
export interface IntrinsicURL extends ImmutableURL {
    // Internal: get the `hash` value.
    getHash(): string;

    // Internal: get the `host` value.
    getHost(): string;

    // Internal: get the `hostname` value.
    getHostname(): string;

    // Internal: get the `href` value.
    getHref(): string;

    // Internal: get the `password` value.
    getPassword(): string;

    // Internal: get the `pathname` value.
    getPathname(): string;

    // Internal: get the `port` value.
    getPort(): number;

    // Internal: get the `protocol` value.
    getProtocol(): string;

    // Internal: get the `search` value.
    getSearch(): string;

    // Internal: get the `searchParams` value.
    // getSearchParams(): IURLSearchParams;

    // Internal: get the `username` value.
    getUsername(): string;

    // Render this URL as a string (same as `toString`).
    toJSON(): string;
}

// Intrinsic-only API surface (mutable)
export interface IntrinsicMutableURL extends IURL {
    // Internal: set the `hash` value.
    setHash(hash: string): void;

    // Internal: set the `host` value.
    setHost(host: string): void;

    // Internal: set the `hostname` value.
    setHostname(hostname: string): void;

    // Internal: set the `href` value.
    setHref(href: string): void;

    // Internal: set the `password` value.
    setPassword(password: string): void;

    // Internal: set the `pathname` value.
    setPathname(pathname: string): void;

    // Internal: set the `port` value.
    setPort(port: number): void;

    // Internal: set the `protocol` value.
    setProtocol(protocol: string): void;

    // Internal: set the `search` value.
    setSearch(search: string): void;

    // Internal: set the `username` value.
    setUsername(username: string): void;
}
