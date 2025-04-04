/**
 * Elide runtime APIs
 *
 * @example
 *
 * ```js
 * import { http } from 'elide';
 * // equivalent to Elide.http
 * ```
 *
 * This module aliases `globalThis.Elide`.
 */
declare module "elide" {
  declare var platform: Platform;
  declare var arch: Architecture;
}

type Platform =
  "android"
  | "darwin"
  | "linux"
  | "win32";

type Architecture = "arm64" | "x64";

type ProcessInfo = {
  readonly platform: Platform,
  readonly arch: Architecture;
}

declare var Elide: {
  readonly version: string;
  readonly process: ProcessInfo;
  readonly http: ElideHttp;
}
